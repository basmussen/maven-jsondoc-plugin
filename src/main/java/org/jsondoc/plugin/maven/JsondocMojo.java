package org.jsondoc.plugin.maven;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.jsondoc.core.pojo.ApiBodyObjectDoc;
import org.jsondoc.core.pojo.ApiDoc;
import org.jsondoc.core.pojo.ApiErrorDoc;
import org.jsondoc.core.pojo.ApiHeaderDoc;
import org.jsondoc.core.pojo.ApiMethodDoc;
import org.jsondoc.core.pojo.ApiObjectDoc;
import org.jsondoc.core.pojo.ApiObjectFieldDoc;
import org.jsondoc.core.pojo.ApiParamDoc;
import org.jsondoc.core.pojo.ApiParamType;
import org.jsondoc.core.pojo.ApiResponseObjectDoc;
import org.jsondoc.core.pojo.ApiVerb;
import org.jsondoc.core.pojo.JSONDoc;
import org.jsondoc.core.util.JSONDocUtils;
import org.sonatype.aether.RepositorySystemSession;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * Goal which generate a xml file using the jsondoc project.
 * 
 * @author Ben Asmussen <info@ben-asmussen.com>
 * @version 1.0.2
 */
@Mojo(name = "jsondoc", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class JsondocMojo extends AbstractMojo
{

    /**
     * Local repository to be used by the plugin to resolve dependencies.
     */
    @Parameter(defaultValue = "${localRepository}")
    protected ArtifactRepository localRepository;

    /**
     * RepositorySystemSession
     */
    @Parameter(defaultValue = "${repositorySystemSession}")
    protected RepositorySystemSession repoSession;

    /**
     * ProjectDependenciesResolver
     */
    @Component
    protected ProjectDependenciesResolver projectDependenciesResolver;

    /**
     * maven project
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> classpathElements;

    /**
     * package to scan, like org.jsondoc.api
     */
    @Parameter(property = "packageToScan", required = true)
    private String packageToScan;

    /**
     * api base path, like http://www.yourapi.com/api/rest/v1/weather
     */
    @Parameter(property = "apiBasePath", required = true)
    private String apiBasePath;

    /**
     * api version
     */
    @Parameter(property = "apiVersion", required = true)
    private String apiVersion;

    /**
     * location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDirectory", required = true)
    private File outputDirectory;

    /**
     * output filename
     */
    @Parameter(defaultValue = "jsondoc.json", property = "outputFile", required = true)
    private String outputFile;

    @Parameter(defaultValue = "json", property = "outputFormat", required = true)
    private String outputFormat;

    public void execute() throws MojoExecutionException
    {
        // cerate output diretctory
        if (!outputDirectory.exists())
        {
            outputDirectory.mkdirs();
        }

        // output file
        File out = new File(outputDirectory, outputFile);

        getLog().info("Using output directory:  " + out);

        FileWriter writer = null;
        try
        {
            writer = new FileWriter(out);

            // configure class loader
            doConfigureClassloader();

            // generate xml output
            doGenerate(writer);

        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Error creating file " + out, e);
        }
        finally
        {
            if (writer != null)
            {
                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                    // ignore
                }
            }
        }
    }

    /**
     * Configure class loader
     * 
     * @throws Exception
     */
    private void doConfigureClassloader() throws Exception
    {
        Set<URL> urls = new HashSet<URL>();
        for (String classpathElement : classpathElements)
        {
            getLog().debug("Class element: " + classpathElement);
            urls.add(new File(classpathElement).toURI().toURL());
        }

        Set<Artifact> dependencyArtifacts = resolveProjectArtifacts(project, repoSession, projectDependenciesResolver);
        for (Artifact artifact : dependencyArtifacts)
        {
            getLog().debug("Project dependency: " + artifact.getFile());
            urls.add(artifact.getFile().toURI().toURL());
        }

        // configure new class loader
        ClassLoader contextClassLoader = URLClassLoader.newInstance(urls.toArray(new URL[0]), Thread.currentThread()
                .getContextClassLoader());

        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }

    /**
     * 
     * @param writer
     * @throws Exception
     */
    private void doGenerate(Writer writer) throws Exception
    {
        getLog().info("Output format: " + outputFormat);
        if ("json".equalsIgnoreCase(outputFormat))
        {
            doGenerate(writer, new JsonHierarchicalStreamDriver());
        }
        else if ("xml".equalsIgnoreCase(outputFormat))
        {
            doGenerate(writer, new StaxDriver());
        }
        else
        {
            throw new IllegalArgumentException("Unsupported output format found: " + outputFile + " Supported formats: xml, json");
        }
    }

    /**
     * Generate output format
     * 
     * @param writer
     * @throws Exception
     */
    private void doGenerate(Writer writer, HierarchicalStreamDriver driver) throws Exception
    {
        List<String> packages = new LinkedList<String>();
        packages.add(packageToScan);

        JSONDoc apiDoc = JSONDocUtils.getApiDoc(apiVersion, apiBasePath, packages);

        // init xstream
        XStream xstream = new XStream(driver);
        xstream.alias("jsondoc", JSONDoc.class);
        xstream.alias("api", ApiDoc.class);
        xstream.alias("apiMethod", ApiMethodDoc.class);
        xstream.alias("apiError", ApiErrorDoc.class);
        xstream.alias("apiBodyObject", ApiBodyObjectDoc.class);
        xstream.alias("apiHeader", ApiHeaderDoc.class);
        xstream.alias("apiObject", ApiObjectDoc.class);
        xstream.alias("apiObjectField", ApiObjectFieldDoc.class);
        xstream.alias("apiParam", ApiParamDoc.class);
        xstream.alias("apiParamType", ApiParamType.class);
        xstream.alias("apiResponseObject", ApiResponseObjectDoc.class);
        xstream.alias("apiVerb", ApiVerb.class);

        xstream.toXML(apiDoc, writer);
    }

    /**
     * Resolve project dependencies
     * 
     * @param project
     * @param repoSession
     * @param projectDependenciesResolver
     * @return
     * @throws MojoExecutionException
     */
    public Set<Artifact> resolveProjectArtifacts(MavenProject project, RepositorySystemSession repoSession,
            ProjectDependenciesResolver projectDependenciesResolver) throws MojoExecutionException
    {

        DefaultDependencyResolutionRequest dependencyResolutionRequest = new DefaultDependencyResolutionRequest(project, repoSession);
        DependencyResolutionResult dependencyResolutionResult;
        try
        {
            dependencyResolutionResult = projectDependenciesResolver.resolve(dependencyResolutionRequest);
        }
        catch (DependencyResolutionException ex)
        {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }

        Set<Artifact> artifacts = new LinkedHashSet<Artifact>();
        if (dependencyResolutionResult.getDependencyGraph() != null
                && !dependencyResolutionResult.getDependencyGraph().getChildren().isEmpty())
        {
            RepositoryUtils.toArtifacts(artifacts, dependencyResolutionResult.getDependencyGraph().getChildren(),
                    Collections.singletonList(project.getArtifact().getId()), null);
        }
        return artifacts;
    }

}
