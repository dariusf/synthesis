package synthesis;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JOptionPane;

public class ColourInformation {
	ArrayList<Color> colours = null;
	ArrayList<ArrayList<String>> keywords = null;
	
	public ColourInformation(String s) {
		colours = new ArrayList<>();
		keywords = new ArrayList<>();
		
		if (s.equals("")) return;

//		String sample = "#ff00ff\nVaruys\nAndra\n\n#red\nSource\n";
//		s = sample;
		
		String[] lines = s.split("\n");
		boolean hasColour = false;
		ArrayList<String> strings = new ArrayList<>();
		
		for (int i=0; i<lines.length; i++) {
			String line = lines[i].trim();
			if (line.equals("") || line.startsWith("//")) {
				continue;
			} else if (line.startsWith("#")) { // new colour
				hasColour = true;
				
				// finalize the previous array of colours
				if (colours.size() > 0) {
					keywords.add(strings);
					strings = new ArrayList<>();
				}
				line = colourAlias(line.substring(1));
				try {
					Color colour = new Color(Integer.parseInt(line.substring(0, 2), 16), Integer.parseInt(line.substring(2, 4), 16), Integer.parseInt(line.substring(4, 6), 16));
					colours.add(colour);
				} catch (Exception e) {
					parseError();
			        return;
				}
			} else { // add to current colour
				if (hasColour) {
					strings.add(line);
				} else {
					parseError();
			        return;
				}
			}
		}
		
		// add the last set of keywords
		keywords.add(strings);
	}
	
	private void parseError() {
        JOptionPane.showMessageDialog(Synthesis.That, "Parse error for highlighted words!");
        colours = new ArrayList<>();
        keywords = new ArrayList<>();
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		
		for (int i=0; i<colours.size(); i++) {
			s.append("Colour: " + colours.get(i) + "\n");
			for (String word : keywords.get(i)) {
				s.append(word + "\n");
			}
			s.append("\n");
		}
		return s.toString();
	}

	private String colourAlias(String alias) {
		if (alias.equals("white")) {
			return "ffffff";
		} else if (alias.equals("silver")) {
			return "c0c0c0";
		} else if (alias.equals("gray")) {
			return "808080";
		} else if (alias.equals("black")) {
			return "000000";
		} else if (alias.equals("red")) {
			return "ff0000";
		} else if (alias.equals("maroon")) {
			return "800000";
		} else if (alias.equals("yellow")) {
			return "ffff00";
		} else if (alias.equals("olive")) {
			return "808000";
		} else if (alias.equals("lime")) {
			return "00ff00";
		} else if (alias.equals("green")) {
			return "008000";
		} else if (alias.equals("aqua")) {
			return "0000ff";
		} else if (alias.equals("teal")) {
			return "008080";
		} else if (alias.equals("blue")) {
			return "0000ff";
		} else if (alias.equals("navy")) {
			return "000080";
		} else if (alias.equals("fuchsia")) {
			return "ff00ff";
		} else if (alias.equals("purple")) {
			return "800080";
		} else {
			return alias;
		}
	}
}
