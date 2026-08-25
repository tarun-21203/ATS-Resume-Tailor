package com.resumetailor.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class ResumePdfGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ResumePdfGenerator.class);

    private static final float MARGIN = 40;
    private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    private static final PDType1Font FONT_REGULAR = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font FONT_ITALIC = new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    public String generatePdfFromJson(String jsonContent) throws IOException {
        logger.info("Generating PDF from JSON, content length: {}", jsonContent.length());
        logger.debug("First 200 chars: {}", jsonContent.substring(0, Math.min(200, jsonContent.length())));

        Gson gson = new Gson();
        JsonObject resume;

        try {
            resume = gson.fromJson(jsonContent, JsonObject.class);
        } catch (Exception e) {
            logger.error("Failed to parse JSON", e);
            throw new IOException("Invalid JSON format: " + e.getMessage(), e);
        }

        if (resume == null || !resume.has("name")) {
            logger.error("Invalid resume JSON structure - missing required fields");
            throw new IOException("Invalid resume JSON structure - missing required fields");
        }

        try {
            resume = gson.fromJson(jsonContent, JsonObject.class);
        } catch (Exception e) {
            System.err.println("Failed to parse JSON: " + e.getMessage());
            throw new IOException("Invalid JSON format: " + e.getMessage(), e);
        }

        if (resume == null || !resume.has("name")) {
            throw new IOException("Invalid resume JSON structure - missing required fields");
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            float yPosition = PAGE_HEIGHT - MARGIN;

            yPosition = drawHeader(contentStream, resume, yPosition);

            if (resume.has("summary")) {
                yPosition = drawSection(contentStream, "SUMMARY", yPosition);
                yPosition = drawParagraph(contentStream, resume.get("summary").getAsString(), yPosition);
            }

            if (resume.has("skills")) {
                yPosition = drawSection(contentStream, "TECHNICAL SKILLS", yPosition);
                yPosition = drawSkills(contentStream, resume.getAsJsonArray("skills"), yPosition);
            }

            if (resume.has("experience")) {
                yPosition = drawSection(contentStream, "EXPERIENCE", yPosition);
                ContentStreamWrapper wrapper = new ContentStreamWrapper(contentStream, yPosition);
                drawExperience(wrapper, resume.getAsJsonArray("experience"), document);
                contentStream = wrapper.stream;
                yPosition = wrapper.yPosition;
            }

            if (resume.has("projects")) {
                yPosition = drawSection(contentStream, "PROJECTS", yPosition);
                ContentStreamWrapper wrapper = new ContentStreamWrapper(contentStream, yPosition);
                drawProjects(wrapper, resume.getAsJsonArray("projects"), document);
                contentStream = wrapper.stream;
                yPosition = wrapper.yPosition;
            }

            if (resume.has("education")) {
                yPosition = drawSection(contentStream, "EDUCATION", yPosition);
                yPosition = drawEducation(contentStream, resume.getAsJsonArray("education"), yPosition);
            }

            if (resume.has("certifications") && resume.get("certifications").isJsonArray()) {
                yPosition = drawSection(contentStream, "CERTIFICATIONS", yPosition);
                yPosition = drawCertifications(contentStream, resume.getAsJsonArray("certifications"), yPosition);
            }

            contentStream.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            logger.info("PDF generated successfully, size: {} bytes", baos.size());
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }

    private static class ContentStreamWrapper {
        PDPageContentStream stream;
        float yPosition;

        ContentStreamWrapper(PDPageContentStream stream, float yPosition) {
            this.stream = stream;
            this.yPosition = yPosition;
        }
    }

    private float drawHeader(PDPageContentStream cs, JsonObject resume, float y) throws IOException {
        cs.setFont(FONT_BOLD, 18);
        String name = cleanText(resume.get("name").getAsString());
        float nameWidth = FONT_BOLD.getStringWidth(name) / 1000 * 18;
        cs.beginText();
        cs.newLineAtOffset((PAGE_WIDTH - nameWidth) / 2, y);
        cs.showText(name);
        cs.endText();
        y -= 25;

        cs.setFont(FONT_REGULAR, 10);
        StringBuilder contact = new StringBuilder();
        if (resume.has("phone"))
            contact.append(resume.get("phone").getAsString()).append(" | ");
        if (resume.has("email"))
            contact.append(resume.get("email").getAsString());
        if (resume.has("linkedin"))
            contact.append(" | ").append(resume.get("linkedin").getAsString());
        if (resume.has("github"))
            contact.append(" | ").append(resume.get("github").getAsString());

        String contactStr = cleanText(contact.toString());
        float contactWidth = FONT_REGULAR.getStringWidth(contactStr) / 1000 * 10;
        cs.beginText();
        cs.newLineAtOffset((PAGE_WIDTH - contactWidth) / 2, y);
        cs.showText(contactStr);
        cs.endText();

        return y - 30;
    }

    private float drawSection(PDPageContentStream cs, String title, float y) throws IOException {
        if (y < 60)
            return y; // Safety check
        cs.setFont(FONT_BOLD, 13);
        cs.beginText();
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(title);
        cs.endText();

        cs.setLineWidth(1f);
        cs.moveTo(MARGIN, y - 3);
        cs.lineTo(PAGE_WIDTH - MARGIN, y - 3);
        cs.stroke();

        return y - 20;
    }

    private float drawParagraph(PDPageContentStream cs, String text, float y) throws IOException {
        return drawWrappedText(cs, cleanText(text), y, MARGIN, false);
    }

    private float drawSkills(PDPageContentStream cs, JsonArray skills, float y) throws IOException {
        for (int i = 0; i < skills.size(); i++) {
            JsonObject skillGroup = skills.get(i).getAsJsonObject();
            String category = skillGroup.get("category").getAsString();
            JsonArray items = skillGroup.getAsJsonArray("items");

            StringBuilder skillLine = new StringBuilder(category + ": ");
            for (int j = 0; j < items.size(); j++) {
                skillLine.append(items.get(j).getAsString());
                if (j < items.size() - 1)
                    skillLine.append(", ");
            }

            y = drawWrappedText(cs, cleanText(skillLine.toString()), y, MARGIN + 10, false);
            y -= 2;
        }
        return y - 5;
    }

    private void drawExperience(ContentStreamWrapper wrapper, JsonArray experiences, PDDocument doc)
            throws IOException {
        for (int i = 0; i < experiences.size(); i++) {
            JsonObject exp = experiences.get(i).getAsJsonObject();

            if (wrapper.yPosition < 100) {
                wrapper.stream.close();
                PDPage newPage = new PDPage(PDRectangle.LETTER);
                doc.addPage(newPage);
                wrapper.stream = new PDPageContentStream(doc, newPage);
                wrapper.yPosition = PAGE_HEIGHT - MARGIN;
            }

            // Job title and dates
            wrapper.stream.setFont(FONT_BOLD, 10);
            wrapper.stream.beginText();
            wrapper.stream.newLineAtOffset(MARGIN, wrapper.yPosition);
            wrapper.stream.showText(cleanText(exp.get("title").getAsString()));
            wrapper.stream.endText();

            String endDate = exp.has("endDate") ? exp.get("endDate").getAsString() : "Present";
            String dates = cleanText(exp.get("startDate").getAsString() + " - " + endDate);
            wrapper.stream.setFont(FONT_REGULAR, 10);
            float dateWidth = FONT_REGULAR.getStringWidth(dates) / 1000 * 10;
            wrapper.stream.beginText();
            wrapper.stream.newLineAtOffset(PAGE_WIDTH - MARGIN - dateWidth, wrapper.yPosition);
            wrapper.stream.showText(dates);
            wrapper.stream.endText();
            wrapper.yPosition -= 11.5f;

            wrapper.stream.setFont(FONT_ITALIC, 10);
            wrapper.stream.beginText();
            wrapper.stream.newLineAtOffset(MARGIN, wrapper.yPosition);
            wrapper.stream
                    .showText(cleanText(exp.get("company").getAsString() + " | " + exp.get("location").getAsString()));
            wrapper.stream.endText();
            wrapper.yPosition -= 11.5f;

            if (exp.has("bullets")) {
                JsonArray bullets = exp.getAsJsonArray("bullets");
                for (int j = 0; j < bullets.size(); j++) {
                    if (wrapper.yPosition < 60) {
                        wrapper.stream.close();
                        PDPage newPage = new PDPage(PDRectangle.LETTER);
                        doc.addPage(newPage);
                        wrapper.stream = new PDPageContentStream(doc, newPage);
                        wrapper.yPosition = PAGE_HEIGHT - MARGIN;
                    }
                    wrapper.yPosition = drawBullet(wrapper.stream, bullets.get(j).getAsString(), wrapper.yPosition);
                }
            }
            wrapper.yPosition -= 8;
        }
    }

    private void drawProjects(ContentStreamWrapper wrapper, JsonArray projects, PDDocument doc) throws IOException {
        for (int i = 0; i < projects.size(); i++) {
            JsonObject project = projects.get(i).getAsJsonObject();

            if (wrapper.yPosition < 100) {
                wrapper.stream.close();
                PDPage newPage = new PDPage(PDRectangle.LETTER);
                doc.addPage(newPage);
                wrapper.stream = new PDPageContentStream(doc, newPage);
                wrapper.yPosition = PAGE_HEIGHT - MARGIN;
            }

            wrapper.stream.setFont(FONT_BOLD, 10);
            wrapper.stream.beginText();
            wrapper.stream.newLineAtOffset(MARGIN, wrapper.yPosition);
            wrapper.stream.showText(cleanText(project.get("name").getAsString()));
            wrapper.stream.endText();

            if (project.has("startDate")) {
                String dates = cleanText(project.get("startDate").getAsString()
                        + (project.has("endDate") ? " - " + project.get("endDate").getAsString() : ""));
                wrapper.stream.setFont(FONT_REGULAR, 10);
                float dateWidth = FONT_REGULAR.getStringWidth(dates) / 1000 * 10;
                wrapper.stream.beginText();
                wrapper.stream.newLineAtOffset(PAGE_WIDTH - MARGIN - dateWidth, wrapper.yPosition);
                wrapper.stream.showText(dates);
                wrapper.stream.endText();
            }
            wrapper.yPosition -= 11.5f;

            if (project.has("technologies")) {
                wrapper.stream.setFont(FONT_ITALIC, 10);
                wrapper.stream.beginText();
                wrapper.stream.newLineAtOffset(MARGIN, wrapper.yPosition);
                wrapper.stream.showText("Technologies: " + cleanText(project.get("technologies").getAsString()));
                wrapper.stream.endText();
                wrapper.yPosition -= 11.5f;
            }

            if (project.has("bullets")) {
                JsonArray bullets = project.getAsJsonArray("bullets");
                for (int j = 0; j < bullets.size(); j++) {
                    if (wrapper.yPosition < 60) {
                        wrapper.stream.close();
                        PDPage newPage = new PDPage(PDRectangle.LETTER);
                        doc.addPage(newPage);
                        wrapper.stream = new PDPageContentStream(doc, newPage);
                        wrapper.yPosition = PAGE_HEIGHT - MARGIN;
                    }
                    wrapper.yPosition = drawBullet(wrapper.stream, bullets.get(j).getAsString(), wrapper.yPosition);
                }
            }
            wrapper.yPosition -= 8;
        }
    }

    private float drawEducation(PDPageContentStream cs, JsonArray education, float y) throws IOException {
        for (int i = 0; i < education.size(); i++) {
            JsonObject edu = education.get(i).getAsJsonObject();

            cs.setFont(FONT_BOLD, 10);
            cs.beginText();
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(cleanText(edu.get("school").getAsString()));
            cs.endText();

            String gradDate = cleanText(edu.get("graduationDate").getAsString());
            float dateWidth = FONT_REGULAR.getStringWidth(gradDate) / 1000 * 10;
            cs.setFont(FONT_REGULAR, 10);
            cs.beginText();
            cs.newLineAtOffset(PAGE_WIDTH - MARGIN - dateWidth, y);
            cs.showText(gradDate);
            cs.endText();
            y -= 11.5f;

            cs.setFont(FONT_ITALIC, 10);
            cs.beginText();
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(cleanText(edu.get("degree").getAsString() + " | " + edu.get("location").getAsString()));
            cs.endText();
            y -= 11.5f;
        }
        return y;
    }

    private float drawCertifications(PDPageContentStream cs, JsonArray certs, float y) throws IOException {
        for (int i = 0; i < certs.size(); i++) {
            y = drawBullet(cs, certs.get(i).getAsString(), y);
        }
        return y - 10;
    }

    private float drawBullet(PDPageContentStream cs, String text, float y) throws IOException {
        cs.setFont(FONT_REGULAR, 10);
        cs.beginText();
        cs.newLineAtOffset(MARGIN + 10, y);
        cs.showText("\u2022");
        cs.endText();
        return drawWrappedText(cs, cleanText(text), y, MARGIN + 22, false);
    }

    private float drawWrappedText(PDPageContentStream cs, String text, float y, float leftMargin, boolean bold)
            throws IOException {
        PDType1Font font = bold ? FONT_BOLD : FONT_REGULAR;
        cs.setFont(font, 10);

        String cleanedText = text.replace("\r", "").replace("\n", " ");
        String[] words = cleanedText.split("\\s+");
        StringBuilder line = new StringBuilder();
        float maxWidth = PAGE_WIDTH - leftMargin - MARGIN;

        for (String word : words) {
            if (word.isEmpty())
                continue;
            String testLine = line + (line.length() > 0 ? " " : "") + word;
            float width = font.getStringWidth(testLine) / 1000 * 10;

            if (width > maxWidth) {
                cs.beginText();
                cs.newLineAtOffset(leftMargin, y);
                cs.showText(line.toString());
                cs.endText();
                y -= 11.5f;
                line = new StringBuilder(word);
            } else {
                line.append(line.length() > 0 ? " " : "").append(word);
            }
        }

        if (line.length() > 0) {
            cs.beginText();
            cs.newLineAtOffset(leftMargin, y);
            cs.showText(line.toString());
            cs.endText();
            y -= 11.5f;
        }

        return y;
    }

    private String cleanText(String text) {
        if (text == null)
            return "";
        return text.replace("\u2013", "-")
                .replace("\u2014", "--")
                .replace("\u2018", "'")
                .replace("\u2019", "'")
                .replace("\u201C", "\"")
                .replace("\u201D", "\"")
                .replace("\u2022", "*")
                .replace("\u00A0", " ")
                .replaceAll("[^\\x00-\\x7F\\x80-\\xFF]", "?");
    }
}
