package uk.co.mikeberinger.mavensitesearch;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import uk.co.mikeberinger.mavensitesearch.config.SearchReplaceWord;
import uk.co.mikeberinger.mavensitesearch.config.SearchStemWord;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.io.File.separator;
import static java.lang.String.format;

/**
 * Provides a client side search for your Maven site pages
 */
@Mojo(name = "maven-site-search", defaultPhase = LifecyclePhase.POST_SITE, threadSafe = true, aggregator = true)
public class SearchMojo extends AbstractMojo {

    private static final String SEARCH_DIR = "search";
    private PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    /**
     * The element containing the content to be searched.
     */
    @Parameter(property = "elementToSearch")
    private String elementToSearch = "main";

    /**
     * The number of results per page.
     */
    @Parameter(property = "resultsPerPage")
    private int resultsPerPage = 10;

    /**
     * The number of words displayed in the results.
     */
    @Parameter(property = "resultsWordCount")
    private int resultsWordCount = 25;

    /**
     * A list of resources to exclude from the search. If omitted, excludes the JavaDocs directory.
     */
    @Parameter(property = "excludedResources", defaultValue = "apidocs")
    private List<String> excludedResources = Collections.singletonList("apidocs");

    /**
     * A list of words to exclude from the search.
     */
    @Parameter(property = "searchStopWords")
    private List<String> searchStopWords = Collections.EMPTY_LIST;

    /**
     * A list of words to replace in the search. Can be useful for words that are commonly referred to differently.
     */
    @Parameter(property = "searchReplaceWords")
    private List<SearchReplaceWord> searchReplaceWords;

    /**
     * A list of words with the same stem in the search. Can be useful for words that are commonly abbreviated.
     */
    @Parameter(property = "searchStemWords")
    private List<SearchStemWord> searchStemWords;

    /**
     * Skip the plugin execution
     */
    @Parameter(property = "skip")
    private boolean skip;

    @Parameter(property = "project")
    private MavenProject project;

    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping plugin execution");
            return;
        }

        if (!project.getModules().isEmpty()) {
            getLog().debug("Detected multi module project");
        }

        String buildDir = project.getBuild().getDirectory();
        File buildDirFile = new File(buildDir);
        if (!buildDirFile.isDirectory()) {
            throw new MojoExecutionException(format("%s is not a directory", buildDir));
        }

        File siteDirFile = new File(buildDir + "/site");
        if (!siteDirFile.exists()) {
            getLog().debug("site directory doesn't exist so creating");
            boolean directoryMade = siteDirFile.mkdirs();
            if (!directoryMade) {
                throw new MojoExecutionException(format("%s directory did not exist and couldn't be created", siteDirFile));
            }
        }

        String siteDir = siteDirFile.toPath().toString();

        copyStaticFilesForSearch(siteDir);
        generateListOfFilesToBeSearched(siteDir);

        getLog().info(format("Search page available at %s", siteDir + "/search.html"));
    }

    private void generateListOfFilesToBeSearched(String siteDir) throws MojoExecutionException {
        getLog().debug(format("Generating list of searchable pages from %s directory", siteDir));

        File folder = new File(siteDir);
        Collection<File> listOfPages = org.apache.commons.io.FileUtils.listFiles(
                folder,
                new String[]{"html"},
                true
        );

        getLog().debug(format("Searchable pages = [%s]", listOfPages));

        List<String> relativePages = listOfPages.stream()
                .filter(file -> !excludedResources.stream()
                        .anyMatch(excludedResource -> file.getAbsolutePath()
                                .contains(excludedResource)))
                .map(file -> "'" + file.getAbsolutePath()
                        .substring(siteDir.length() + 1) + "'")
                .collect(Collectors.toList());
        String searchPages = String.join(", ", relativePages);

        getLog().debug(format("After excluding [%s], searchable pages = [%s]", excludedResources, searchPages));

        StringBuilder stringBuilder = new StringBuilder()
                .append("$(document).ready(function() {" + System.lineSeparator())
                .append(format("  tipuesearch_stop_words = [%s];%s", constructStopWordsList(), System.lineSeparator()))
                .append(format("  tipuesearch_replace = {'words': [%s]};%s", constructReplaceWordsList(), System.lineSeparator()))
                .append(format("  tipuesearch_stem = {'words': [%s]};%s", constructStemWordsList(), System.lineSeparator()))
                .append(format("  tipuesearch_pages = [%s];%s", searchPages, System.lineSeparator()))
                .append("  $('#tipue_search_input').tipuesearch({" + System.lineSeparator())
                .append("    'mode': 'live'," + System.lineSeparator())
                .append(format("    'show': %s,%s", resultsPerPage, System.lineSeparator()))
                .append(format("    'descriptiveWords': %s,%s", resultsWordCount, System.lineSeparator()))
                .append(format("    'liveDescription': '%s',%s", elementToSearch, System.lineSeparator()))
                .append(format("    'liveContent': '%s'%s", elementToSearch, System.lineSeparator()))
                .append("  });" + System.lineSeparator())
                .append("});" + System.lineSeparator() + System.lineSeparator());

        try {
            Files.write(Paths.get(siteDir + separator + "js" + separator + "searchConfig.js"), stringBuilder.toString()
                            .getBytes(),
                    StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create search config file");
        }
    }

    private String constructStopWordsList() {
        return searchStopWords.stream()
                .map(word -> format("'%s'", word))
                .collect(Collectors.joining(", "));
    }

    private String constructReplaceWordsList() {
        return searchReplaceWords.stream()
                .map(searchReplaceWord -> format("{ 'word': '%s', 'replace_with': '%s'}", searchReplaceWord.getWord(), searchReplaceWord.getReplaceWith()))
                .collect(Collectors.joining(", "));
    }

    private String constructStemWordsList() {
        return searchStemWords.stream()
                .map(searchStemWord -> format("{ 'word': '%s', 'stem': '%s'}", searchStemWord.getWord(), searchStemWord.getStem()))
                .collect(Collectors.joining(", "));
    }

    private void copyStaticFilesForSearch(String siteDir) throws MojoExecutionException {
        getLog().debug(format("Copying static search files into %s directory", siteDir));

        copyStaticFilesFromSearchDirectory(siteDir, null);
        copyStaticFilesFromSearchDirectory(siteDir, "img");
        copyStaticFilesFromSearchDirectory(siteDir, "js");
    }

    private void copyStaticFilesFromSearchDirectory(String siteDir, String subdirectory)
            throws MojoExecutionException {
        try {
            String location = subdirectory != null
                    ? "classpath:" + SEARCH_DIR + separator + subdirectory + separator + "*"
                    : "classpath:" + SEARCH_DIR + separator + "*";
            Resource[] directoryResources = resourcePatternResolver.getResources(location);

            for (Resource resource : directoryResources) {
                String outputPath = subdirectory != null ? siteDir + separator + subdirectory + separator
                        + resource.getFilename() : siteDir + separator + resource.getFilename();
                FileUtils.copyURLToFile(resource.getURL(), new File(outputPath));
            }
        } catch (IOException e) {
            throw new MojoExecutionException(format("Could not copy necessary search files to %s directory", siteDir), e);
        }
    }
}
