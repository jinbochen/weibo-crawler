package edu.bit.dlde.weibo_crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * @author lins 2012-6-25
 */
public class Test {

	/**
	 * @param args
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static void main(String[] args) throws IOException, SAXException,
			ParserConfigurationException {
		Date d = new Date();
		System.out.println(d.getTime());
		String data = "";
		BufferedReader br = new BufferedReader(new FileReader(new File(
				"/home/lins/test")));
		while (true) {
			String tmp = br.readLine();
			if (tmp == null)
				break;
			data += tmp;
		}
		data="<html><body>"+data+"</body></html>";
		HtmlCleaner cleaner = new HtmlCleaner();
		cleaner.getProperties().setNamespacesAware(false);
		Document doc = null;
		try {
			doc = new DomSerializer(cleaner.getProperties(), true)
					.createDOM(cleaner.clean(data));
		} catch (ParserConfigurationException e1) {
		}
		NodeList nodeList = doc.getElementsByTagName("body").item(0)
				.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node n = nodeList.item(i);
			if(n.getNodeType() != Node.ELEMENT_NODE)
				continue;
			DOMSource source = new DOMSource(n);
			StringWriter writer = new StringWriter();
			Result result = new StreamResult(writer);

			Transformer transformer = null;
			try {
				transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

			} catch (TransformerConfigurationException e) {

			} catch (TransformerFactoryConfigurationError e) {

			}

			try {
				transformer.transform(source, result);
			} catch (TransformerException e) {
				return;
			}

			System.out.println(writer);
			try {
				writer.close();
			} catch (IOException e) {
			}
		}
	}

}
