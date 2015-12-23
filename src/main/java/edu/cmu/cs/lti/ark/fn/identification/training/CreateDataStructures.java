package edu.cmu.cs.lti.ark.fn.identification.training;

import com.google.common.io.Files;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by alessio on 22/12/15.
 */

public class CreateDataStructures {

    /**
     * This class generates the two original maps (from FrameNet)
     * The frameMap file maps the frame to lemmas invoking that frame (with POS)
     * For example: Experiencer_focus => {loathe_VB, delighted_VBD, resented_VBD, ... }
     * The elementMap file maps the frame to its roles
     * For example: Experiencer_focus => {Topic, Circumstances, Parameter, ... }
     * Both maps are THashMap<String, THashSet<String>>
     * <p>
     * Arguments:
     * - FrameNet parsed training file (all.lemma.tags)
     * - FrameNet frames training file (frame.elements)
     * - FrameNet frame folder
     * - Output FrameNet map
     * - Output frame element map
     *
     * @param args Arguments (see above)
     */
    public static void main(String[] args) {

        try {
            String lemmaFile = args[0];
            String frameFile = args[1];
            String frameFolder = args[2];
            String outFrameMap = args[3];
            String outElementsMap = args[4];

            BufferedReader reader;
            String line;

            THashMap<String, THashSet<String>> frameMap = new THashMap<String, THashSet<String>>();
            THashMap<String, THashSet<String>> elementMap = new THashMap<String, THashSet<String>>();

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            System.out.println("[INFO] Reading frames");
            for (final File file : Files.fileTreeTraverser().preOrderTraversal(new File(frameFolder))) {
                if (!file.isFile()) {
                    continue;
                }
                if (file.getName().startsWith(".")) {
                    continue;
                }
                if (!file.getName().endsWith(".xml")) {
                    continue;
                }

                Document doc = dBuilder.parse(file);
                doc.getDocumentElement().normalize();

                String frameName = doc.getDocumentElement().getAttribute("name");
                THashSet<String> roles = new THashSet<String>();

                HashSet<NodeList> lists = new HashSet<NodeList>();
                lists.add(doc.getElementsByTagName("FE"));
                lists.add(doc.getElementsByTagName("memberFE"));

                for (NodeList list : lists) {
                    for (int temp = 0; temp < list.getLength(); temp++) {
                        Node nNode = list.item(temp);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) nNode;
                            roles.add(eElement.getAttribute("name"));
                        }
                    }

                }

                elementMap.put(frameName, roles);
                frameMap.put(frameName, new THashSet<String>());
            }

            System.out.println("[INFO] Loading sentences");
            reader = new BufferedReader(new FileReader(lemmaFile));
            int sentNo = -1;
            HashMap<Integer, HashMap<Integer, String>> tokenPos = new HashMap<Integer, HashMap<Integer, String>>();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                String[] parts = line.split("\t");

                try {
                    int tokensNo = Integer.parseInt(parts[0]);
                    if (parts.length < tokensNo * 6 + 1) {
                        throw new Exception("Invalid row");
                    }

                    sentNo++;
                    tokenPos.put(sentNo, new HashMap<Integer, String>());
                    for (int i = 1; i < 1 + tokensNo; i++) {
                        String tokPos = parts[i] + "_" + parts[i + tokensNo];
                        tokenPos.get(sentNo).put(i - 1, tokPos);
                    }
                } catch (Exception e) {
                    continue;
                }

            }
            reader.close();

            System.out.println("[INFO] Loading frames");
            reader = new BufferedReader(new FileReader(frameFile));

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                String[] parts = line.split("\t");

                try {
                    if (parts.length < 6) {
                        throw new Exception("Invalid row");
                    }

                    String frameName = parts[1];
                    Integer sentence = Integer.parseInt(parts[5]);
                    String framePosition = parts[3];

                    String res;
                    try {
                        Integer id = Integer.parseInt(framePosition);
                        res = tokenPos.get(sentence).get(id);
                    } catch (NumberFormatException e) {
                        String[] lemmaParts = parts[2].split("\\.");
                        res = parts[4] + "_" + lemmaParts[lemmaParts.length - 1];
                    }

                    try {
                        frameMap.get(frameName).add(res);
                    }
                    catch (NullPointerException e) {
                        // ignored
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            reader.close();

            System.out.println(frameMap.get("Experiencer_focus"));
            System.out.println(elementMap.get("Experiencer_focus"));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
