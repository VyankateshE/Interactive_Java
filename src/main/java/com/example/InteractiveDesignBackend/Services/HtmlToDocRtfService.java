package com.example.InteractiveDesignBackend.Services;

import com.spire.doc.Document;

import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.spire.doc.FileFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

@Service
public class HtmlToDocRtfService {

	public byte[] convertHtmlToDoc(InputStream htmlInputStream) throws Exception {

		Document document = new Document();
		document.loadFromStream(htmlInputStream, FileFormat.Html);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		document.saveToStream(outputStream, FileFormat.Doc);
		return outputStream.toByteArray();
	}

	public byte[] convertHtmlToRtf(InputStream htmlInputStream) throws Exception {
		String html = new String(htmlInputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

		html = inlineAllCss(html);

		Document document = new Document();
		document.loadFromStream(new ByteArrayInputStream(html.getBytes()), FileFormat.Html);

		ByteArrayOutputStream docxStream = new ByteArrayOutputStream();
		document.saveToStream(docxStream, FileFormat.Docx);

		Document docxDoc = new Document();
		docxDoc.loadFromStream(new ByteArrayInputStream(docxStream.toByteArray()), FileFormat.Docx);

//        fixTableStructure(docxDoc);

		ByteArrayOutputStream rtfStream = new ByteArrayOutputStream();
		docxDoc.saveToStream(rtfStream, FileFormat.Rtf);

		return rtfStream.toByteArray();
	}

	private String inlineAllCss(String html) throws IOException {
		
		
		org.jsoup.nodes.Document doc = Jsoup.parse(html);
		doc.outputSettings().prettyPrint(false);

		doc.select("link[rel=stylesheet]").remove();
		
		for (Element table : doc.select("table")) {
            table.after("<br/><br/>");
        }

		String manualBootstrap = """
				<style>
				  .btn {
				    display: inline-block;
				    font-weight: 400;
				    text-align: center;
				    border: 1px solid transparent;
				    padding: 6px 12px;
				    font-size: 14px;
				    line-height: 1.5;
				    border-radius: 4px;
				    margin: 3px;
				    text-decoration: none;
				  }
				  .btn-success {
				    color: #fff;
				    background-color: #28a745;
				    border-color: #28a745;
				  }
				  .btn-danger {
				    color: #fff;
				    background-color: #dc3545;
				    border-color: #dc3545;
				  }
				  .btn-warning {
				    color: #212529;
				    background-color: #ffc107;
				    border-color: #ffc107;
				  }
				  .btn-info {
				    color: #fff;
				    background-color: #17a2b8;
				    border-color: #17a2b8;
				  }
				  .btn-light {
				    color: #212529;
				    background-color: #f8f9fa;
				    border-color: #f8f9fa;
				  }
				  .btn-dark {
				    color: #fff;
				    background-color: #343a40;
				    border-color: #343a40;
				  }
				</style>
				""";
		doc.head().append(manualBootstrap);

		applyInlineButtonStyles(doc);

		return doc.outerHtml();
	}

	private void applyInlineButtonStyles(org.jsoup.nodes.Document doc) {
		String baseStyle = "display:inline-block;font-weight:400;text-align:center;border:1px solid transparent;"
				+ "padding:6px 12px;font-size:14px;line-height:1.5;border-radius:4px;margin:3px;text-decoration:none;";

		for (org.jsoup.nodes.Element btn : doc.select(".btn")) {
			String style = baseStyle;
			if (btn.hasClass("btn-success"))
				style += "color:#fff;background-color:#28a745;border-color:#28a745;";
			else if (btn.hasClass("btn-danger"))
				style += "color:#fff;background-color:#dc3545;border-color:#dc3545;";
			else if (btn.hasClass("btn-warning"))
				style += "color:#212529;background-color:#ffc107;border-color:#ffc107;";
			else if (btn.hasClass("btn-info"))
				style += "color:#fff;background-color:#17a2b8;border-color:#17a2b8;";
			else if (btn.hasClass("btn-light"))
				style += "color:#212529;background-color:#f8f9fa;border-color:#f8f9fa;";
			else if (btn.hasClass("btn-dark"))
				style += "color:#fff;background-color:#343a40;border-color:#343a40;";
			else
				style += "background-color:#e0e0e0;color:#000;";

			String existing = btn.attr("style");
			btn.attr("style", existing.isEmpty() ? style : existing + ";" + style);
		}
	}

}



//public byte[] convertHtmlToRtf(InputStream htmlInputStream) throws Exception {
//String html = new String(htmlInputStream.readAllBytes(), StandardCharsets.UTF_8);
//String processedHtml = preprocessHtmlForRtf(html);
//
//Document document = new Document();
//document.loadFromStream(
//  new ByteArrayInputStream(processedHtml.getBytes(StandardCharsets.UTF_8)), 
//  FileFormat.Html
//);
//
//ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//document.saveToStream(outputStream, FileFormat.Rtf);
//return outputStream.toByteArray();
//}
//
//private String preprocessHtmlForRtf(String html) {
//org.jsoup.nodes.Document doc = Jsoup.parse(html);
//
//for (Element th : doc.select("th[id^=json-table-c1301-colheader-0-1]")) {
//  th.attr("style", th.attr("style") + "; background-color: #c43939; color: #fdfcfc;");
//}
//
//for (Element elem : doc.select("[id^=json-table-c1301-colheader], [id^=json-table-c1301-rowheader], [id^=json-table-c1301-cell]")) {
//  String id = elem.id();
//
//  if (id.equals("json-table-c1301-colheader-0-1")) {
//      elem.attr("style", elem.attr("style") + "; background-color: #c43939; color: #fdfcfc;");
//  } else if (id.equals("iormn") || id.equals("i8917") || id.equals("ib9ah")) {
//      elem.attr("style", elem.attr("style") + "; background-color: #f8f9fa;");
//  } else if (id.equals("iz1pj") || id.equals("ixnz9") || id.equals("i5e34")) {
//      elem.attr("style", elem.attr("style") + "; background-color: #ffffff;");
//  }
//}
//
//for (Element button : doc.select("button.btn")) {
//  String style = "padding: 1000px 20px; border: 1px solid #ccc; border-radius: 4px;";
//  if (button.hasClass("btn-primary")) {
//      style += " background-color: #007bff; color: white;";
//  } else if (button.hasClass("btn-danger")) {
//      style += " background-color: #dc3545; color: white;";
//  }
//  button.attr("style", button.attr("style") + "; " + style);
//}
//
//return doc.html();
//}

//private String preprocessHtmlForRtf(String html) {
//  org.jsoup.nodes.Document doc = Jsoup.parse(html);
//  
//  // Convert Bootstrap buttons to inline styles
//  for (Element button : doc.select("button.btn")) {
//      String style = "padding: 10px 20px; border: 1px solid #ccc; border-radius: 4px;";
//      
//      if (button.hasClass("btn-primary")) {
//          style += " background-color: #007bff; color: white;";
//      } else if (button.hasClass("btn-secondary")) {
//          style += " background-color: #6c757d; color: white;";
//      } else if (button.hasClass("btn-success")) {
//          style += " background-color: #28a745; color: white;";
//      } else if (button.hasClass("btn-danger")) {
//          style += " background-color: #dc3545; color: white;";
//      } else if (button.hasClass("btn-warning")) {
//          style += " background-color: #ffc107; color: black;";
//      } else if (button.hasClass("btn-info")) {
//          style += " background-color: #17a2b8; color: white;";
//      } else if (button.hasClass("btn-light")) {
//          style += " background-color: #f8f9fa; color: black;";
//      } else if (button.hasClass("btn-dark")) {
//          style += " background-color: #343a40; color: white;";
//      }
//      
//      button.attr("style", button.attr("style") + "; " + style);
//  }
//  
//  // Apply padding from CSS
//  for (Element elem : doc.select("#ijds")) {
//      elem.attr("style", elem.attr("style") + "; padding: 20px; background-color: #455fa2; color: #e90e0e;");
//  }
//  
//  return doc.html();
//}

//public byte[] convertHtmlToRtf(InputStream htmlInputStream) throws Exception {
//  Document document = new Document();
//  document.loadFromStream(htmlInputStream, FileFormat.Html);
//
//  ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//  document.saveToStream(outputStream, FileFormat.Rtf);
//  return outputStream.toByteArray();
//}

//public byte[] convertHtmlToRtf(InputStream htmlInputStream) throws Exception {
//  // Step 1: Read HTML
//  String html = new String(htmlInputStream.readAllBytes(), StandardCharsets.UTF_8);
//  org.jsoup.nodes.Document doc = Jsoup.parse(html);
//
//  // Step 2: Fetch external CSS files and inline them
//  for (Element link : doc.select("link[rel=stylesheet]")) {
//      String cssUrl = link.attr("href");
//      if (cssUrl.startsWith("http")) {
//          try (InputStream cssStream = new URL(cssUrl).openStream()) {
//              String css = new String(cssStream.readAllBytes(), StandardCharsets.UTF_8);
//              doc.head().append("<style>" + css + "</style>");
//          } catch (Exception e) {
//              System.out.println("Failed to fetch CSS: " + cssUrl);
//          }
//      }
//      link.remove();
//  }
//
//  // Step 3: Convert HTML (now with inline CSS) to RTF
//  com.spire.doc.Document document = new com.spire.doc.Document();
//  document.loadFromStream(new ByteArrayInputStream(doc.html().getBytes()), com.spire.doc.FileFormat.Html);
//
//  ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//  document.saveToStream(outputStream, com.spire.doc.FileFormat.Rtf);
//  return outputStream.toByteArray();
//}

//public byte[] convertHtmlToRtf(InputStream htmlInputStream) throws Exception {
//  // Step 1️⃣: Read HTML
//  String html = new String(htmlInputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
//
//  // Step 2️⃣: Inline all CSS (convert <style> and linked CSS into inline styles)
//  html = inlineAllCss(html);
//
//  // Step 3️⃣: Convert HTML → DOCX → RTF (preserves more colors)
//  Document document = new Document();
//  document.loadFromStream(new ByteArrayInputStream(html.getBytes()), FileFormat.Html);
//
//  ByteArrayOutputStream docxStream = new ByteArrayOutputStream();
//  document.saveToStream(docxStream, FileFormat.Doc);
//
//  Document docxDoc = new Document();
//  docxDoc.loadFromStream(new ByteArrayInputStream(docxStream.toByteArray()), FileFormat.Doc);
//
//  // Optional: fix table alignment/layout without changing borders
////  fixTableStructure(docxDoc);
//
//  ByteArrayOutputStream rtfStream = new ByteArrayOutputStream();
//  docxDoc.saveToStream(rtfStream, FileFormat.Rtf);
//
//  return rtfStream.toByteArray();
//}
//
//
//private String inlineAllCss(String html) throws IOException {
//org.jsoup.nodes.Document doc = Jsoup.parse(html);
//doc.outputSettings().prettyPrint(false);
//
//// Remove all <link> stylesheets
//doc.select("link[rel=stylesheet]").remove();
//
//// Add custom manual Bootstrap approximation
//String manualBootstrap = """
//<style>
//.btn {
//  display: inline-block;
//  font-weight: 400;
//  text-align: center;
//  border: 1px solid transparent;
//  padding: 6px 12px;
//  font-size: 14px;
//  line-height: 1.5;
//  border-radius: 4px;
//  margin: 3px;
//  text-decoration: none;
//}
//.btn-success {
//  color: #fff;
//  background-color: #28a745;
//  border-color: #28a745;
//}
//.btn-danger {
//  color: #fff;
//  background-color: #dc3545;
//  border-color: #dc3545;
//}
//.btn-warning {
//  color: #212529;
//  background-color: #ffc107;
//  border-color: #ffc107;
//}
//.btn-info {
//  color: #fff;
//  background-color: #17a2b8;
//  border-color: #17a2b8;
//}
//.btn-light {
//  color: #212529;
//  background-color: #f8f9fa;
//  border-color: #f8f9fa;
//}
//.btn-dark {
//  color: #fff;
//  background-color: #343a40;
//  border-color: #343a40;
//}
//</style>
//""";
//doc.head().append(manualBootstrap);
//
//// ✅ Apply those button styles inline
//applyInlineButtonStyles(doc);
//
//return doc.outerHtml();
//}
//
//private void applyInlineButtonStyles(org.jsoup.nodes.Document doc) {
//// Base button style
//String baseStyle = "display:inline-block;font-weight:400;text-align:center;border:1px solid transparent;"
//      + "padding:6px 12px;font-size:14px;line-height:1.5;border-radius:4px;margin:3px;text-decoration:none;";
//
//// Apply to all .btn
//for (org.jsoup.nodes.Element btn : doc.select(".btn")) {
//  String style = baseStyle;
//  if (btn.hasClass("btn-success"))
//      style += "color:#fff;background-color:#28a745;border-color:#28a745;";
//  else if (btn.hasClass("btn-danger"))
//      style += "color:#fff;background-color:#dc3545;border-color:#dc3545;";
//  else if (btn.hasClass("btn-warning"))
//      style += "color:#212529;background-color:#ffc107;border-color:#ffc107;";
//  else if (btn.hasClass("btn-info"))
//      style += "color:#fff;background-color:#17a2b8;border-color:#17a2b8;";
//  else if (btn.hasClass("btn-light"))
//      style += "color:#212529;background-color:#f8f9fa;border-color:#f8f9fa;";
//  else if (btn.hasClass("btn-dark"))
//      style += "color:#fff;background-color:#343a40;border-color:#343a40;";
//  else
//      style += "background-color:#e0e0e0;color:#000;";
//
//  // Merge with existing inline style (if any)
//  String existing = btn.attr("style");
//  btn.attr("style", existing.isEmpty() ? style : existing + ";" + style);
//}
//}

// Simple inliner that merges <style> rules into elements
//private String inlineAllCss(String html) {
//  org.jsoup.nodes.Document doc = Jsoup.parse(html);
//  doc.outputSettings().prettyPrint(false);
//
//  // Keep <style> rules — Spire.Doc will now read them better
//  // Also strip external CSS links
//  doc.select("link[rel=stylesheet]").remove();
//  return doc.outerHtml();
//}
