/*
 * Copyright (c) Brandon Sanders [brandon@alicorn.io]
 */
package io.alicorn.wp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO:
 *
 * @author Brandon Sanders [brandon@alicorn.io]
 */
public class WordpressConverter {
//Private//////////////////////////////////////////////////////////////////////

    // List of common language tags in files.
    private static final Set<String> CODE_TAGS = new HashSet<>();
    static {
        CODE_TAGS.add("[java]");
        CODE_TAGS.add("[/java]");
    }

    // Pattern matcher for notice tags.
    private static final Pattern NOTICE_PATTERN = Pattern.compile("\\s*\\Q[notice]\\E(.*)\\Q[/notice]\\E\\s*");

    // Caption cleaner regex.
    private static final Pattern CAPTION_CLEANER_PATTERN = Pattern.compile("\\Q[![\\E.*?]\\(.*?\\)]\\(.*?\\)");

    // Pattern matcher for captions.
    private static final Pattern CAPTION_BLOCK_PATTERN = Pattern.compile("\\Q[caption\\E.*?](.*)\\Q[/caption]\\E");
    private static final Pattern CAPTION_ENTRY_PATTERN = Pattern.compile("\\s*(?=\\Q[!\\E)");
    private static final Pattern CAPTION_PATTERN = Pattern.compile(".*\\Q(http://www.mechakana.com/blog\\E(.*?)\\Q)\\E\\s*(.*)");

//Protected////////////////////////////////////////////////////////////////////

//Public///////////////////////////////////////////////////////////////////////

    public static void main(String[] args) throws IOException {

        // Parse file name.
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected exactly one argument: a file name to convert!\n" +
                                               "Usage: java -jar wpc.jar [path to file]");
        }
        String fileName = args[0];

        // Prepare output file.
        if (fileName.startsWith("./")) {
            fileName = fileName.substring(2);
        }
        String newFileName = fileName.split("\\Q.\\E")[0] + ".md";
        Path file = Paths.get(newFileName);
        BufferedWriter fileWriter = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

        // Before processing file contents, we need to process front-matter.
        // 0 - First header not seen (---).
        // 1 - First header seen.
        // 2 - Last header seen (---).
        int frontMatterProgress = 0;

        // Parse file line-by-line.
        for (String line : Files.readAllLines(Paths.get(fileName))) {

            // Remove trailing new lines.
            if (line.endsWith("\n")) {
                line = line.substring(0, line.length() - 2);
            }

            // Process front matter first!
            if (frontMatterProgress != 2) {

                // Front matter not processed.
                if (frontMatterProgress == 0 && line.equals("---")) {

                    // Begin front matter.
                    frontMatterProgress = 1;
                    fileWriter.write("---\n");

                    // Add image information.
                    fileWriter.write("image:\n" +
                                           "    feature: \"/img/mechakana-import.jpg\"\n" +
                                           "    feature-credit: \"Brandon Sanders\"\n");

                // Front matter processing.
                } else if (frontMatterProgress == 1 && !line.equals("---")) {

                    // Only preserve title.
//                    if (line.startsWith("title")) {
//                        fileWriter.write(line + "\n");
//                    }

                // Front matter done.
                } else {

                    // Close front matter.
                    if (frontMatterProgress == 1) {
                        fileWriter.write("---\n");
                    }

                    // Finish front matter.
                    frontMatterProgress = 2;
                }

                continue;
            }

            // Is it a code tag?
            if (CODE_TAGS.contains(line)) {
                fileWriter.write("```\n");
                continue;
            }

            // Is it a notice?
            Matcher noticeMatcher = NOTICE_PATTERN.matcher(line);
            if (noticeMatcher.find()) {
                fileWriter.write("## " + noticeMatcher.group(1) + "\n");
                continue;
            }

            // Is it a caption?
            Matcher captionBlockMatcher = CAPTION_BLOCK_PATTERN.matcher(line);
            if (captionBlockMatcher.find()) {

                // Extract all captions.
                String[] captions = captionBlockMatcher.group(1).split(CAPTION_ENTRY_PATTERN.pattern());

                // Process every caption.
                for (String caption : captions) {
                    Matcher captionMatcher = CAPTION_PATTERN.matcher(caption);
                    if (captionMatcher.find()) {
                        fileWriter.write("\n![" + captionMatcher.group(2) + "](" + captionMatcher.group(1) + ")\n");
                    }
                }

                continue;
            }

            // TODO: Do the thing!
            List<String> matches = new ArrayList<>();
            Matcher matcher = CAPTION_CLEANER_PATTERN.matcher(line);
            while (matcher.find()) {
                // Get the match.
                String match = matcher.group();

                // Check if it's a caption.
                Matcher captionMatcher = CAPTION_PATTERN.matcher(match);
                if (captionMatcher.find()) {
                    matches.add("\n![" + captionMatcher.group(2) + "](" + captionMatcher.group(1) + ")\n");
                } else {
                    matches.add(match);
                }
            }
            matches.add(matcher.replaceAll(""));

            // Add all matches.
            for (String match : matches) {

                // Prevent H1 tags from existing.
                if (match.startsWith("# ")) {
                    match = "#" + match;
                }

                // Add line.
                fileWriter.write(match + "\n");
            }
        }

        // Flush buffer.
        fileWriter.flush();
        fileWriter.close();
    }
}
