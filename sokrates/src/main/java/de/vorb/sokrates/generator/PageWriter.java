package de.vorb.sokrates.generator;

import de.vorb.sokrates.cli.GenerateCommand;
import de.vorb.sokrates.db.jooq.tables.pojos.Page;
import de.vorb.sokrates.db.repositories.PageRepository;
import de.vorb.sokrates.db.repositories.PageTagRepository;
import de.vorb.sokrates.db.repositories.TagRepository;
import de.vorb.sokrates.generator.pandoc.PandocRunner;
import de.vorb.sokrates.generator.pandoc.PandocSourceFileFormat;
import de.vorb.sokrates.model.PageMetaData;
import de.vorb.sokrates.model.SourceFileMatch;
import de.vorb.sokrates.properties.SokratesProperties;
import de.vorb.sokrates.properties.SourceFileRuleProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.io.Files.getFileExtension;
import static com.google.common.io.Files.getNameWithoutExtension;
import static de.vorb.sokrates.generator.pandoc.PandocTargetFileFormat.HTML5;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

@Slf4j
@Component
@RequiredArgsConstructor
public class PageWriter {

    private final GenerateCommand generateCommand;
    private final PageRepository pageRepository;
    private final PageTagRepository pageTagRepository;
    private final TagRepository tagRepository;
    private final SokratesProperties sokratesProperties;
    private final SourceFileFinder sourceFileFinder;
    private final PageMetaDataParser pageMetaDataParser;
    private final Sha1ChecksumCalculator sha1ChecksumCalculator;
    private final PandocRunner pandocRunner;
    private final PebbleRenderer pebbleRenderer;

    @Transactional
    public void writePages() {
        final List<SourceFileRuleProperties> generateRules = sokratesProperties.getGenerator().getGenerateRules();
        final List<SourceFileMatch> sourceFileMatches =
                sourceFileFinder.findSourceFileMatches(generateRules).collect(Collectors.toList());

        int numberOfWrittenPages = 0;
        for (final SourceFileMatch sourceFileMatch : sourceFileMatches) {

            final Path sourceFilePath = sourceFileMatch.getFilePath();
            final byte[] checksum = sha1ChecksumCalculator.calculateChecksum(sourceFilePath);
            final Page existingPage = pageRepository.fetchOneBySourceFilePath(sourceFilePath);

            if (!generateCommand.isForce() && isPageNewOrChanged(checksum, existingPage)) {
                log.debug("Skipping file {} because it did not change");
                continue;
            }

            final PageMetaData pageMetaData = parsePageMetaData(sourceFileMatch);
            if (pageMetaData == null) {
                continue;
            }

            createMissingTags(pageMetaData.getTags());

            final Path outputFilePath = determineOutputFilePath(sourceFileMatch, pageMetaData);

            final Page page = createPage(sourceFileMatch, pageMetaData, outputFilePath, checksum);

            renderPage(page, pageMetaData);

            storePage(page, existingPage);

            linkPageToTags(page, pageMetaData.getTags());

            numberOfWrittenPages++;
        }

        log.info("Wrote {} pages", numberOfWrittenPages);
    }

    private boolean isPageNewOrChanged(byte[] newChecksum, Page existingPage) {
        return existingPage != null && Arrays.equals(newChecksum, existingPage.getChecksum());
    }

    private void createMissingTags(Set<String> tags) {
        final int numberOfCreatedTags = tagRepository.saveTags(tags);
        log.info("Created {} missing tags", numberOfCreatedTags);
    }

    private Path determineOutputFilePath(SourceFileMatch sourceFileMatch, PageMetaData pageMetaData) {
        final Path sourceFileName = Optional.ofNullable(pageMetaData.getAlias())
                .map(Paths::get)
                .orElse(sourceFileMatch.getFilePath().getFileName());

        final Path relativePath = sourceFileMatch.getBaseDirectoryPath().relativize(sourceFileMatch.getFilePath());
        final Path outputDirectory = sokratesProperties.getDirectory().getOutput()
                .resolve(relativePath)
                .getParent();
        ensureOutputDirectoryExists(outputDirectory);

        final Path outputFileName = mapFileExtension(sourceFileName);
        return outputDirectory.resolve(outputFileName);
    }

    private Path mapFileExtension(Path fileName) {
        final Map<String, String> extensionMapping = sokratesProperties.getGenerator().getExtensionMapping();
        final String fileNameString = fileName.toString();

        final String fileExtension = getFileExtension(fileNameString);
        if (fileExtension.isEmpty() || !extensionMapping.containsKey(fileExtension)) {
            return fileName;
        } else {
            final String fileNameWithoutExtension = getNameWithoutExtension(fileNameString);
            return Paths.get(fileNameWithoutExtension + '.' + extensionMapping.get(fileExtension));
        }
    }

    @Nullable
    private PageMetaData parsePageMetaData(SourceFileMatch sourceFileMatch) {

        final Path sourceFilePath = sourceFileMatch.getFilePath();

        try (final Reader reader = openReader(sourceFilePath)) {

            final PageMetaData pageMetaData = pageMetaDataParser.parseMetaDataFrom(reader);

            if (pageMetaData.getLocale() == null) {
                pageMetaData.setLocale(sokratesProperties.getSite().getDefaultLocale());
            }

            return pageMetaData;

        } catch (IOException e) {
            log.error("Could not read file {}", sourceFilePath);
            return null;
        }
    }

    private BufferedReader openReader(Path file) throws IOException {
        return Files.newBufferedReader(file, UTF_8);
    }

    private void ensureOutputDirectoryExists(Path outputDirectory) {
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            log.error("Unable to create output directory: {}", outputDirectory);
        }
    }

    private Page createPage(SourceFileMatch sourceFileMatch, PageMetaData pageMetaData, Path outputFilePath,
            byte[] checksum) {
        return new Page()
                .setSourceFilePath(sourceFileMatch.getFilePath())
                .setSourceFileFormat(sourceFileMatch.getFormat().getFormat())
                .setOutputFilePath(outputFilePath)
                .setTitle(pageMetaData.getTitle())
                .setCreatedAt(pageMetaData.getCreatedAt())
                .setLastModifiedAt(pageMetaData.getLastModifiedAt())
                .setLocale(pageMetaData.getLocale())
                .setChecksum(checksum);
    }

    private void renderPage(Page page, PageMetaData pageMetaData) {

        final Locale locale = getDocumentLocale(page);
        final String htmlContent =
                pandocRunner.convertFile(page.getSourceFilePath(), locale,
                        PandocSourceFileFormat.forString(page.getSourceFileFormat()), HTML5);

        final Path outputFilePath = page.getOutputFilePath();
        try (final Writer writer = openWriter(outputFilePath)) {
            pebbleRenderer.renderPage(writer, pageMetaData, htmlContent);
            log.info("Rendered file {} to {}", page.getSourceFilePath(), outputFilePath);
        } catch (IOException e) {
            log.error("Could not write file {}", outputFilePath);
        }

    }

    private Locale getDocumentLocale(Page page) {
        return Optional.ofNullable(page.getLocale())
                .orElse(sokratesProperties.getSite().getDefaultLocale());
    }

    private BufferedWriter openWriter(Path file) throws IOException {
        return Files.newBufferedWriter(file, UTF_8, CREATE, TRUNCATE_EXISTING);
    }

    private void storePage(Page page, Page existingPage) {
        if (existingPage == null) {
            pageRepository.insert(page);
        } else {
            page.setId(existingPage.getId());
            pageRepository.update(page);
        }
    }

    private void linkPageToTags(Page page, Set<String> tags) {

        final Long pageId = pageRepository.fetchOneBySourceFilePath(page.getSourceFilePath()).getId();

        pageTagRepository.savePageTags(pageId, tags);
    }

}
