# Maven site search

This plugin provides a client-side search for your Maven sites. It can be useful for when you can't provide a proper server side solution. The JQuery library that provides the functionality is [Tipue search](https://www.jqueryscript.net/other/jQuery-Site-Search-Engine-Plugin-Tipue-Search.html). By default, the plugin is bound to the `post-site` phase so that it can search pages generated in the `site` phase.

## Usage

Basic usage using defaults:
```xml
 <plugin>
    <groupId>uk.co.mikeberinger</groupId>
    <artifactId>maven-site-search</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>maven-site-search</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Advanced usage using all options:
```xml
<plugin>
    <groupId>uk.co.mikeberinger</groupId>
    <artifactId>maven-site-search</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>maven-site-search</goal>
            </goals>
            <configuration>
                <skip>false</skip>
                <elementToSearch>main</elementToSearch>
                <resultsPerPage>10</resultsPerPage>
                <resultsWordCount>10</resultsWordCount>
                <excludedResources>
                    <excludedResource>apidocs</excludedResource>
                    <excludedResource>jacoco</excludedResource>
                    <excludedResource>dependency-check-report.html</excludedResource>
                </excludedResources>
                <searchStopWords>
                    <searchStopWord>and</searchStopWord>
                    <searchStopWord>the</searchStopWord>
                </searchStopWords>
                <searchReplaceWords>
                    <searchReplaceWord>
                        <word>organization</word>
                        <replaceWith>organisation</replaceWith>
                    </searchReplaceWord>
                </searchReplaceWords>
                <searchStemWords>
                    <searchStemWord>
                        <word>documentation</word>
                        <stem>docs</stem>
                    </searchStemWord>
                </searchStemWords>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Options

| Name               | Description                                                                                                  | Type                    | Required                             |
|--------------------|--------------------------------------------------------------------------------------------------------------|-------------------------|--------------------------------------|
| elementToSearch    | The element containing the content to be searched.                                                           | String                  | No. Defaults to 'main' (fluido skin) |
| resultsPerPage     | The number of results per page.                                                                              | int                     | No. Defaults to 10                   |
| resultsWordCount   | The number of words displayed in the results.                                                                | int                     | No. Defaults to 25                   |
| excludedResources  | A list of resources to exclude from the search.                                                              | List<String>            | No. Defaults to 'apidocs'            |
| searchStopWords    | A list of words to exclude from the search.                                                                  | List<String>            | No. Defaults to empty list           |
| searchReplaceWords | A list of words to replace in the search. Can be useful for words that are commonly referred to differently. | List<SearchReplaceWord> | No. Defaults to empty list           |
| searchStemWords    | A list of words with the same stem in the search. Can be useful for words that are commonly abbreviated.     | List<SearchStemWord>    | No. Defaults to empty list           |
| skip               | Skip plugin execution                                                                                        | boolean                 | No. Defaults to false                |

## Usage warning

The auto generated JavaDocs are excluded from the search, because this would cause performance issues and the report already provides a search feature. However, if your site build automatically generates other large reports (e.g. Jacoco), you will face the same problem and should configure this via the `excludedResources` option. 

## Multi-module projects

When this plugin is configured in the parent POM for a Maven multi-module project, it will generate a search function specific for each module/project. So in the parent project, the search will **not** cover pages within the child modules, you will need to access their site for that.