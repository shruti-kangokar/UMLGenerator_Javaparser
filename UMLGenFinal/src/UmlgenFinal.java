import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.Parameter;

public class UmlgenFinal {
	public static String s = "@startuml\n" + "skinParam classAttributeIconSize 0 \n";
	public static String clName;
	public static String cname = "";
	public static int mc = 0;
	public static String fineRead;
	public static Map<String, String> associationDepMap = new HashMap();
	public static Map<String, String> depMapForMethod = new HashMap();

	public static void main(String[] args) throws Exception {
		// creates an input stream for the file to be parsed
		File dir = new File(args[0]);
		if (args.length > 0) {
			File[] ditList = dir.listFiles();
			if (ditList != null) {
				for (File ch : ditList) {
					if ((ch.getName().contains(".java"))) {
						fineRead = ch.toString();
						File file = new File(ch.getAbsolutePath());
						FileInputStream in = new FileInputStream(file);
						CompilationUnit cu;
						try {
							cu = JavaParser.parse(in, "UTF8");
						} finally {
							in.close();
						}
						String temp = cu.toString();
						String line[] = temp.split("\\r?\\n");
						String delims = "[ .,?!]+";
						int lc = 0;
						while (line[lc].matches("^\\s*import\\s+.*") || line[lc].matches("^$")) {
							lc++;
						}
						String[] tokens = line[lc].split(delims);
						List<TypeDeclaration> types = cu.getTypes();
						TypeDeclaration typeDec = types.get(0);
						clName = typeDec.getName();
						if (tokens[1].equals("interface"))
							s = s + "interface" + " " + clName + "\n";
						if (tokens[1].equals("class"))
							s = s + "class" + " " + clName + "\n";
						new FindingConstructors().visit(cu, null);
						// new FieldVisitor().visit(cu, null);
						new VisitingMethods().visit(cu, null);
						new VisitingClassOrInter().visit(cu, null);
						new FindingClassOrInterType().visit(cu, null);
						// new VariableVisitor().visit(cu, null);
						new ModifyAccessSpec().visit(cu, null);
					}
				}
				s = s + "@enduml\n";
				//System.out.println(s);
				//System.out.println("---------------------------------------");
				String newS = modifyForAsssoc(s);
				//System.out.println("---------------------------------------");

			//	System.out.println(newS);
				//System.out.println("---------------------------------------");
				String newS2 = modifyForDep(newS);
				System.out.println(newS2);
				String destination=args[1];
				generateUML p = new generateUML();
				p.generateUml(newS2,destination);
			}
		}
	}

	private static String modifyForAsssoc(String s2) {
		Pattern primitives = Pattern.compile(".*(byte|short|int|long|float|double|boolean|char|String)(\\[.*\\])?.*");
		String retStr = "";
		String value = "";
		String[] splitStr = s2.split("\\r?\\n");
		for (int i = 0; i < splitStr.length; i++) {
			if (splitStr[i].contains("//")) {
				continue;
			}
			Matcher m = primitives.matcher(splitStr[i]);
			if (m.find()) {
			} else {
				String[] tmp = splitStr[i].split("\\s+");

				if ((tmp.length == 6) && !(splitStr[i].contains("(") && splitStr[i].contains(")"))) {
					// Modify third Element
					tmp[5] = tmp[5].replaceAll("Collection", "\"*\"");
					tmp[5] = tmp[5].replaceAll("List", "");
					tmp[5] = tmp[5].replaceAll("<", "");
					tmp[5] = tmp[5].replaceAll(">", "");
					tmp[5] = tmp[5].replaceAll("\\[", "");
					tmp[5] = tmp[5].replaceAll("\\]", "");
					String str9 = "";
					if (tmp[5].contains("*")) {
						str9 = tmp[5].substring(3, tmp[5].length());
					} else {
						str9 = tmp[5];
					}
					String key = srtdString(tmp[0], str9);
					value = tmp[0] + "-" + tmp[5];
					if (associationDepMap.containsKey(key)) {
						if (associationDepMap.get(key).equals(value)) {
						} else if (associationDepMap.get(key).length() > value.length()) {
							System.out.println(" Keyequal1  " + associationDepMap.get(key));
							// do Nothing
						} else if (associationDepMap.get(key).length() < value.length()) {
							// Replace the value
							associationDepMap.put(key, value);
						} else if (associationDepMap.get(key).length() == value.length()) {
							// Check if new or current value has a *
							if (value.contains("*")) {
								String str10 = new StringBuilder(value).insert(1, "\"*\"").toString();
								associationDepMap.put(key, str10);
							} else {
								// Do nothing
							}
						}
					} else {
						associationDepMap.put(key, value);
					}
				} else {
					continue;
				}
			}
		}
		//System.out.println("Printing hash map ");
		for (String k : associationDepMap.keySet()) {
		}
		//System.out.println("---------------------------------------");
		for (int i = 0; i < splitStr.length; i++) {
			if (splitStr[i].contains("//")) {
				continue;
			}

			String rmColl[] = splitStr[i].split("\\s+");
			if(rmColl.length==6 && !(splitStr[i].contains("(") && splitStr[i].contains(")"))) 
			{
				Matcher m = primitives.matcher(rmColl[5]);
				if (!m.find()) {
				//	System.out.println(" #### Eliminating the line "+splitStr[i]);
					continue;
				}
			}
			
			if (splitStr[i].equals("@enduml")) {
				for (String k : associationDepMap.keySet()) {
					retStr = retStr + associationDepMap.get(k) + "\n";
				}
				retStr = retStr + "@enduml";
			} else {
				if (!splitStr[i].contains("#")) {
					retStr = retStr + splitStr[i] + "\n";
				}
			}
		}
		return retStr;
	}

	private static class FieldVisitor extends VoidVisitorAdapter {
		@Override
		public void visit(FieldDeclaration n, Object arg) {
			String k = n.toString(); // System.out.println("K: " +k);
			k = k.replaceAll("[;]", "");
			String[] strs = k.split("\\s+");
			if (strs[0].equals("public"))
				strs[0] = "+";
			if (strs[0].equals("private"))
				strs[0] = "-";
			if (strs[0].equals("protected"))
				strs[0] = "#";
			if (strs.length == 2) {
				s = s + clName + " : public " + strs[0] + " " + strs[1];
			} else {
				s = s + clName + " : " + strs[0] + " " + strs[1] + " " + strs[2];
			}
			s = s + "\n";
			super.visit(n, arg);
		}
	}
	public static String srtdString(String s1, String s2) {
		String a = s1;
		String b = s2;
		String small = "";
		String big = "";
		int size;
		String key;
		if (a.length() < b.length()) {
			size = a.length();
			small = a;
			big = b;
		} else {
			size = b.length();
			small = b;
			big = a;
		}
		for (int i = 0; i < size; i++) {
			if (a.charAt(i) < b.charAt(i)) {
				small = a;
				big = b;
				break;
			} else if (a.charAt(i) > b.charAt(i)) {
				small = b;
				big = a;
				break;
			} else {
				continue;
			}
		}
		key = small + big;
		return key;
	}
	private static class ObjectTypeFinder extends VoidVisitorAdapter {
		@Override
		public void visit(ClassOrInterfaceDeclaration decl, Object arg) {
			// Make class extend 
			List<ClassOrInterfaceType> list = decl.getImplements();
			if (list == null)
				return;
			for (ClassOrInterfaceType k : list) {
				String n = k.toString();
				s = s + n + " " + "<|.." + " " + clName + "\n";
			}
		}
	}
	
	private static class ModifyAccessSpec extends VoidVisitorAdapter {
		@Override
		public void visit(FieldDeclaration n, Object arg) {
			int Modi = n.getModifiers();
			String access = "";
			String dType = n.getType().toString();
			String VarName = n.getVariables().toString();
			String Var = VarName.substring(1, VarName.length() - 1);

			if (Var.contains("new")) {
				String newObjArray[] = Var.split("\\s+");

				Var = newObjArray[0];
				  }

			if (Modi == 1) // public
			{
				access = "+";
				s = s + clName + " : " + access + " " + Var + " : " + dType;
				s = s + "\n";
			} else if (Modi == 2) // private
			{
				List<String> ar = new ArrayList<String>();
				ar = VisitingMethods.st;
				String MethMsg = "";
				String VarMsg = "";
				int Len = 0;
				if (ar.size() != 0) {
					for (int i = 0; i < ar.size(); i++) {
						MethMsg = ar.get(i).toString();
						Len = MethMsg.length();
						VarMsg = MethMsg.substring(3, Len);
						if (VarMsg.equalsIgnoreCase(Var)) {
							access = "+";
						} else {
							access = "-";
						}
					}
					s = s + clName + " : " + " " + access + " " + Var + " : " + dType;
					s = s + "\n";
				} else {
					access = "-";
					//System.out.println(" Printing > " + clName + " : " + access + " " + Var + " : " + dType);
					s = s + clName + " : " + " " + access + " " + Var + " : " + dType;
					s = s + "\n";
				}
			} else if (Modi == 4) // protected
			{
				access = "#";
				s = s + clName + " : " + " " + access + " " + Var + " : " + dType;
				s = s + "\n";
			} else if (Modi == 0) // package scope
			{
			}
			super.visit(n, arg);
		}
	}
	
	private static class VisitingMethods extends VoidVisitorAdapter {
		public static List<String> st = new ArrayList();
		public static List<String> body = new ArrayList();

		@Override
		public void visit(MethodDeclaration n, Object arg) {
			// BlockStmt block = n.getBody();
			if (n.getBody() != null) {
				String stBody = n.getBody().toString();
				String trimmed;
				String line[] = stBody.split("\\n+");
				for (int i = 0; i < line.length; i++) {
					if (line[i].contains("new")) {
						int indexNew = line[i].toString().indexOf("new");
						{
							if (indexNew > 0) {
								trimmed = line[i].toString().trim();
								body.add(trimmed);
								//System.out.println("Line with new is " + trimmed);
							}
						}
					} else {

					}
				}
				String str = "";
				String str1 = "";
				String str2 = "";
				for (int j = 0; j < body.size(); j++) {
					str2 = body.get(j).toString();
					String splitter[] = str2.split("\\s+");
					str1 = splitter[0];

					// Write Grammar
					String buildkey = srtdString(clName, str1);
					if (depMapForMethod.containsKey(buildkey)) {
						// Do nothing
					} else {
						String value = clName + "\"uses\"..>" + str1;
						depMapForMethod.put(buildkey, value);
						//System.out.println(" Storing val "+value);
					}
				}
			}
			
			String method = n.getName();
			if (!(method.startsWith("get") || method.startsWith("set"))) {
				String op = n.getDeclarationAsString(true, false, false);
				String[] parts = op.split(" ");
				String accessSpecifier = null;
				if (parts[0].equalsIgnoreCase("public"))
					accessSpecifier = "+";
				else if (parts[0].equalsIgnoreCase("private"))
					accessSpecifier = "-";
				else if (parts[0].equalsIgnoreCase("protected"))
					accessSpecifier = "#";
				else
					accessSpecifier = "+";
				String parameter = null;
				String dataType = n.getType().toString();
				List<Parameter> param01 = n.getParameters();
				parameter = param01.toString();
				int paramLength = parameter.length();
				String pa = parameter.substring(1, paramLength - 1);
				if (accessSpecifier == "+")
					s = s + clName + " : " + accessSpecifier + " " + method + "(" + pa + ")" + " : " + dataType;
				s = s + "\n";
				// System.out.println("-------------");
			} else {
				st.add(method);
			}
		}
	}
	private static class FindingClassOrInterType extends VoidVisitorAdapter {
		@Override
		public void visit(ClassOrInterfaceDeclaration decl, Object arg) {
			String n = "";
			List<ClassOrInterfaceType> list = decl.getImplements();
			if (list == null) {
				  return;
			}
			for (ClassOrInterfaceType k : list) {
				n = k.toString();
				
				  s = s + n + " " + "<|.." + " " + clName + "\n";
				cname = cname + " " + n;
			}
		}
	}
	private static class FindingConstructors extends VoidVisitorAdapter {
		@Override
		public void visit(ConstructorDeclaration n, Object arg) {
			String method = n.getName();
			String ConsDecl = n.getDeclarationAsString(true, false, false);
			String DefaultCons = method + "()";
			// if (!(ConsDecl.equalsIgnoreCase(DefaultCons))) {
			String op = n.getDeclarationAsString(true, false, false);
			String[] parts = op.split(" ");
			String accessSpecifier = null;
			if (parts[0].equalsIgnoreCase("public"))
				accessSpecifier = "+";
			else if (parts[0].equalsIgnoreCase("private"))
				accessSpecifier = "-";
			else if (parts[0].equalsIgnoreCase("protected"))
				accessSpecifier = "#";
			else
				accessSpecifier = "+";
			String parameter = null;
			List<Parameter> param01 = n.getParameters();
			//for (int i = 0; i < param01.size(); i++) {
				//System.out.println(param01.get(i));
			//}
			parameter = param01.toString();
			int paramLength = parameter.length();
			String pa = parameter.substring(1, paramLength - 1);
			if (accessSpecifier == "+")
				s = s + clName + " : " + accessSpecifier + " " + method + "(" + pa + ")";
			s = s + "\n";
		}
	}

	private static class VisitingClassOrInter extends VoidVisitorAdapter {
		@Override
		public void visit(ClassOrInterfaceDeclaration decl, Object arg) {
			// Make class extend//
			String n1 = "";

			List<ClassOrInterfaceType> list = decl.getExtends();
			if (list == null) {
				return;
			}
			for (ClassOrInterfaceType k : list) {
				n1 = k.toString();
				 s = s + n1 + " " + "<|--" + " " + clName + "\n";
			}
			cname = cname + " " + n1 + " " + clName;
		}
	}

	private static String modifyForDep(String s) {
		/*
		 * 
		 * 0. split the UML String at new line and for each line do the below 1.
		 * Match the pattern (....) 2. Check the contents inside the paranthesis
		 * for non primitive datatypes. 3. for each of the non primitive data
		 * types argument we need a line in UML. 4. Example Pattern for the line
		 * -> C1..>A1
		 * 
		 */

		String retStr = "";
		String appendStr = "";

		// Extract all dependencies from the hashmap.
		for (String k : depMapForMethod.keySet()) {
			appendStr = appendStr + depMapForMethod.get(k) + "\n" ;
		}
		//System.out.println("Current dependencies are: " + appendStr);
		
		String[] splitString = s.split("\\r?\\n");
		for (int i = 0; i < splitString.length; i++) {
			if (splitString[i].contains("//")) {
				continue;
			}

			// Always need the method grammar in UML string.
			if (splitString[i].contains("@enduml")) {
				continue;
			}
			retStr = retStr + splitString[i] + "\n";

			// Get the class name
			String[] tmpStr = splitString[i].split("\\s+");
			String clname = tmpStr[0];
			// 1
			if (splitString[i].contains("(") && splitString[i].contains(")")) {
				int startIndex = splitString[i].indexOf('(');
				int endIndex = splitString[i].indexOf(')');
				String args = splitString[i].substring(startIndex + 1, endIndex);
				if (args.length() == 0) {
				} else {
					// 2
					String argArray[] = args.split(",");
					Pattern primitives = Pattern
							.compile(".*(byte|short|int|long|float|double|boolean|char|String)(\\[.*\\])?.*");
					for (int j = 0; j < argArray.length; j++) {
						// if string in parantheses is primitive type, ignore
						String varType = argArray[j].split("\\s+")[0];
						Matcher m = primitives.matcher(varType);
						if (m.find()) {
							// the argument is of primitive type. Do nothing
							continue;
						} else {
							String keyStr = srtdString(clname, varType);
								// TODO: Search before adding
								String newAppend = clname + "\"uses\"..>" + varType;
								Pattern ptn = Pattern.compile(newAppend);
								Matcher m2 = ptn.matcher(appendStr);
								if (!m2.find()) {
									// This dep has not been seen before so add
									// it
									// The argumnet is not primitive but it is
									// an
									// object of a differnt class
									appendStr = appendStr + clname + "\"uses\"..>" + varType + "\n";
								} else {
									// We have already added this dependency.
									// Do not add again.
								}
							//}
						}
					}
				}
			} else {
				// Add enduml after concatenating the appendStr
				// If it is not our interesting line.
				// retStr = retStr + splitString[i] + "\n";
			}
		}
		return retStr + appendStr + "@enduml";
	}
}
