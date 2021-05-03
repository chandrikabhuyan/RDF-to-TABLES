import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import utils.Sessionutil;

import java.io.InputStream;
import java.util.*;
public class rdf_final {
    public void CreateTables(HashMap<String, List<String>> tables, HashMap<String, String> types, HashMap<String, List<String>> references, HashMap<String, String> copyRef) {
        List<String> all_q = new ArrayList<String>();
        Iterator<Map.Entry<String, List<String>>> iterator = tables.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<String>> next = iterator.next();
            String name = next.getKey();
            String query = "CREATE TABLE IF NOT EXISTS " + name + "(";
            Iterator<String> iterator1 = next.getValue().iterator();
            while (iterator1.hasNext()) {
                String col = iterator1.next();
                query += col + " " + types.get(col);
                if (col.equals(name + "_ID")) {
                    query += " PRIMARY KEY ";
                }
                query += ",";
            }
            query = query.substring(0, query.length() - 1);
            query += ")";
            all_q.add(query);
        }

        Iterator<String> iterator1 = all_q.iterator();
        while (iterator1.hasNext()) {
            Session session = Sessionutil.getSession();
            Transaction transaction = session.beginTransaction();
            String next = iterator1.next();
            System.out.println(next);
            Query query = session.createSQLQuery(next);
            query.executeUpdate();
            transaction.commit();
        }
        List<String> updateQueries = new ArrayList<String>();
        Iterator<Map.Entry<String, List<String>>> iterator2 = references.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry<String, List<String>> next = iterator2.next();
            String name = next.getKey();
            String query = "ALTER TABLE " + name;
            Iterator<String> iterator3 = next.getValue().iterator();
            int flag = 1;
            while (iterator3.hasNext()) {
                String next1 = iterator3.next();
                if (copyRef.containsKey(name) && flag == 1) {
                    query += " ADD FOREIGN KEY (" + next1 + "_ID_REF) REFERENCES " + next1 + "(" + next1 + "_ID),";
                    flag = 0;
                } else {
                    query += " ADD FOREIGN KEY (" + next1 + "_ID) REFERENCES " + next1 + "(" + next1 + "_ID),";
                }
            }
            query = query.substring(0, query.length() - 1);
            updateQueries.add(query);
        }

        Iterator<String> iterator3 = updateQueries.iterator();
        while (iterator3.hasNext()) {
            Session session = Sessionutil.getSession();
            Transaction transaction = session.beginTransaction();
            String next = iterator3.next();
            Query query = session.createSQLQuery(next);
            query.executeUpdate();
            transaction.commit();
        }
    }

    public void insertData(String query) {
        Session session = Sessionutil.getSession();
        Transaction transaction = session.beginTransaction();
        Query query1 = session.createSQLQuery(query);
        query1.executeUpdate();
        transaction.commit();
    }

    public static void main(String[] args) {
        // create an empty model
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        String ns = "http://www.w3.org/2002/07/owl#";
        // use the RDFDataMgr to find the input file
        InputStream in = RDFDataMgr.open("src/main/resources/alt.owl");
        if (in == null) {
            throw new IllegalArgumentException(
                    "File: sample.rdf +  not found");
        }

// read the RDF/XML file
        model.read(in, null);
        List<String> objProperties = new ArrayList<String>();
        HashMap<String, List<String>> refInt = new HashMap<String, List<String>>();
        HashMap<String, String> URItoName = new HashMap<String, String>();
        HashMap<String, List<String>> TableColumn = new HashMap<String, List<String>>();
        HashMap<String, String> ColumnTypes = new HashMap<String, String>();
        HashMap<String, String> copyRef = new HashMap<String, String>();

        ////////////////////////////////////////
        // Parsing owl file for classes
        ExtendedIterator<OntClass> iterator = model.listClasses();
        System.out.println("/**************************************************************/");
        while (iterator.hasNext()) {
            OntClass ontClass = (OntClass) iterator.next();
            URItoName.put(ontClass.getLocalName(), ontClass.getLabel("en"));
            List<String> cols = new ArrayList<String>();
            String str;
            if (ontClass.getLabel("en") != null) {
                str = ontClass.getLabel("en");
            } else {
                str = ontClass.getLocalName();
            }
            cols.add(str + "_ID");
            ColumnTypes.put(str + "_ID", "VARCHAR(30)");
            System.out.println("Class is : " + str);
            if (ontClass.hasSuperClass()) {
                List<String> superClass = new ArrayList<String>();
                ExtendedIterator<OntClass> classes = ontClass.listSuperClasses();
                while (classes.hasNext()) {
                    OntClass next = classes.next();
                    String sup;
                    if (next.getLabel("en") != null) {
                        sup = next.getLabel("en");
                    } else {
                        sup = next.getLocalName();
                    }
                    superClass.add(sup);
                    cols.add(sup + "_ID");
                }
                refInt.put(str, superClass);
            }
            TableColumn.put(str, cols);
        }

        /////////////////////////////////////////////
        // Parsing owl file for object properties


        System.out.println("/**************************************************************/");
        ExtendedIterator<ObjectProperty> nodeIterator = model.listObjectProperties();
        while (nodeIterator.hasNext()) {
            ObjectProperty objectProperty = (ObjectProperty) nodeIterator.next();
            String opname;
            if (objectProperty.getLabel("en") == null)
                opname = objectProperty.getLocalName();
            else
                opname = objectProperty.getLabel("en");
            System.out.println("Object property is : " + opname);
            objProperties.add(opname);
            List<String> cols = new ArrayList<String>();
            URItoName.put(objectProperty.getLocalName(), objectProperty.getLabel("en"));
//            System.out.println("Domain is: " + objectProperty.getDomain().toString());
            ExtendedIterator<? extends OntProperty> extendedIterator = objectProperty.listInverseOf();
            if (extendedIterator.hasNext()) {
                String iObj, iDomain, iRange;
                if (objectProperty.getInverseOf().getLabel("en") != null)
                    iObj = objectProperty.getInverseOf().getLabel("en");
                else
                    iObj = objectProperty.getInverseOf().getLocalName();

                if (objectProperty.getInverseOf().getRange().getLabel("en") != null)
                    iDomain = objectProperty.getInverseOf().getRange().getLabel("en");
                else
                    iDomain = objectProperty.getInverseOf().getRange().getLocalName();

                if (objectProperty.getInverseOf().getDomain().getLabel("en") != null)
                    iRange = objectProperty.getInverseOf().getDomain().getLabel("en");
                else
                    iRange = objectProperty.getInverseOf().getDomain().getLocalName();


                System.out.println("Inverse Object property of: " + iObj);
                System.out.println("Domain is: " + iDomain);
                System.out.println("Range is: " + iRange);
                cols.add(TableColumn.get(iDomain).get(0));
                cols.add(TableColumn.get(iRange).get(0));
                List<String> references;
                if (refInt.containsKey(opname))
                    references = refInt.get(opname);
                else
                    references = new ArrayList<String>();
                references.add(iDomain);
                references.add(iRange);
                refInt.put(opname, references);
                TableColumn.put(opname, cols);
            } else {
                String domain, range;
                if (objectProperty.getDomain().getLabel("en") == null)
                    domain = objectProperty.getDomain().getLocalName();
                else
                    domain = objectProperty.getDomain().getLabel("en");
                if (objectProperty.getRange().getLabel("en") == null)
                    range = objectProperty.getRange().getLocalName();
                else
                    range = objectProperty.getRange().getLabel("en");
                System.out.println("Domain is: " + domain);
                if (TableColumn.get(domain).get(0).equals(TableColumn.get(range).get(0))) {
                    copyRef.put(opname, TableColumn.get(range).get(0) + "_REF");
                    cols.add(TableColumn.get(domain).get(0));
                    cols.add(TableColumn.get(range).get(0) + "_REF");
                    ColumnTypes.put(TableColumn.get(range).get(0) + "_REF", "VARCHAR(30)");
                } else {
                    cols.add(TableColumn.get(domain).get(0));
                    cols.add(TableColumn.get(range).get(0));
                }
                System.out.println("Range is: " + range);
                TableColumn.put(opname, cols);
                List<String> str1;
                if (refInt.containsKey(opname)) {
                    str1 = refInt.get(opname);
                } else
                    str1 = new ArrayList<String>();
                str1.add(domain);
                str1.add(range);
                refInt.put(opname, str1);
            }

        }

        ////////////////////////////////////////////////////
        // Parsing owl file for Datatype Properties


        System.out.println("/**************************************************************/");
        ExtendedIterator<DatatypeProperty> extendedIterator = model.listDatatypeProperties();
        while (extendedIterator.hasNext()) {
            DatatypeProperty datatypeProperty = (DatatypeProperty) extendedIterator.next();
            String dpname, domain, range;
            if (datatypeProperty.getLabel("en") == null)
                dpname = datatypeProperty.getLocalName();
            else
                dpname = datatypeProperty.getLabel("en");
            System.out.println("Datatype property is: " + dpname);
            URItoName.put(datatypeProperty.getLocalName(), datatypeProperty.getLabel("en"));
            if (datatypeProperty.getRange().getLabel("en") == null)
                range = datatypeProperty.getRange().getLocalName();
            else
                range = datatypeProperty.getRange().getLabel("en");
            ExtendedIterator<? extends OntResource> extendedIterator1 = datatypeProperty.listDomain();
            while (extendedIterator1.hasNext()) {
                OntResource next = extendedIterator1.next();
                if (next.getLabel("en") == null)
                    domain = next.getLocalName();
                else
                    domain = next.getLabel("en");
                System.out.println("Domain is: " + domain);
                ExtendedIterator<OntClass> classes = next.asClass().listSubClasses();
                while (classes.hasNext()) {
                    OntClass next1 = classes.next();
                    String subClass;
                    if (next1.getLabel("en") == null)
                        subClass = next1.getLocalName();
                    else
                        subClass = next1.getLabel("en");
                    List<String> strings = TableColumn.get(subClass);
                    strings.add(dpname);
                    TableColumn.put(subClass, strings);
                }
                List<String> str = TableColumn.get(domain);
                str.add(dpname);
                TableColumn.put(domain, str);
            }
            String type;
            if (range.equals("string"))
                type = "VARCHAR(30)";
            else
                type = range;
            ColumnTypes.put(dpname, type);
            System.out.println("Range is: " + range);
        }


        ///////////////////////////////////////////
        // Printing all Data that has been parsed


        System.out.println("/**************************************************************/");
        Iterator<Map.Entry<String, List<String>>> it = TableColumn.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<String>> next = it.next();
            System.out.print("Table is: " + next.getKey() + " Columns are: ");
            List<String> value = next.getValue();
            Iterator<String> iterator3 = value.iterator();
            while (iterator3.hasNext()) {
                System.out.print(iterator3.next() + " ");
            }
            System.out.println();
        }


        System.out.println("/**************************************************************/");
        Iterator<Map.Entry<String, List<String>>> iterator1 = refInt.entrySet().iterator();
        while (iterator1.hasNext()) {
            Map.Entry<String, List<String>> next = iterator1.next();
            System.out.print("PK Table is: " + next.getKey() + " FK Tables are: ");
            List<String> value = next.getValue();
            Iterator<String> it1 = value.iterator();
            while (it1.hasNext()) {
                System.out.print(it1.next() + " ");
            }
            System.out.println();
        }

        System.out.println("/**************************************************************/");

        Iterator<Map.Entry<String, String>> iterator3 = ColumnTypes.entrySet().iterator();
        while (iterator3.hasNext()) {
            Map.Entry<String, String> next = iterator3.next();
            System.out.println("Column is: " + next.getKey() + " with Type: " + next.getValue());
        }
        System.out.println("Transitive properties---------------------");
        ExtendedIterator transitiveProperties = model.listTransitiveProperties();
        HashMap<String, List<String>> tp = new HashMap<String, List<String>>();
        while (transitiveProperties.hasNext()) {
            TransitiveProperty t = (TransitiveProperty) transitiveProperties.next();
            System.out.println(t.getLocalName() + t.getDomain().getLocalName() + t.getRange().getLocalName());
            String Subject=t.getLocalName();
            String domain=t.getDomain().getLocalName();
            String range=t.getRange().getLocalName();
            tp.put(t.getLocalName(), new ArrayList<String>());
            tp.get(t.getLocalName()).add(t.getDomain().getLocalName());
            tp.get(t.getLocalName()).add(t.getRange().getLocalName());
            List<String>cols=new ArrayList<String>();

            objProperties.add(t.getLocalName());
            cols.add(TableColumn.get(domain).get(0));
            cols.add(TableColumn.get(range).get(0));
            List<String> references;
            references = new ArrayList<String>();
            references.add(domain);
            references.add(range);
            refInt.put(t.getLocalName(), references);
            TableColumn.put(Subject,cols);
        }
        rdf_final rdf = new rdf_final();
        rdf.CreateTables(TableColumn, ColumnTypes, refInt, copyRef);
        // Structure eg map<T001,map<first_name,xyz last_name,abc>>
        HashMap<String, HashMap<String, String>> dataItems = new HashMap<String, HashMap<String, String>>();
        //Structure eg map<Teaches,map<T001,[CS506,AI702]>>
        HashMap<String, HashMap<String, List<String>>> objItems = new HashMap<String, HashMap<String, List<String>>>();
        HashMap<String, String> tableItems = new HashMap<String, String>();
        //For inferencing/////////////////////////////////////////////////
        HashMap<String, List<String>> stp = new HashMap<String, List<String>>();
        HashMap<List<String>, List<String>> pto = new HashMap<List<String>, List<String>>();
        HashMap<String, List<String>> rev = new HashMap<String, List<String>>();
        HashMap<String, String> inverse = new HashMap<String, String>();
        HashMap<String, List<List<String>>> PredTo = new HashMap<String, List<List<String>>>();
        HashMap<String, String> domains = new HashMap<String, String>();
        //////////Infer//////////////////////////////
        StmtIterator stmtIterator = model.listStatements();
        while (stmtIterator.hasNext()) {
            Statement statement = stmtIterator.nextStatement();
            Resource subject = statement.getSubject();
            Property predicate = statement.getPredicate();
            RDFNode object = statement.getObject();
            String sub, pred, obj;
            if (URItoName.containsKey(subject.getLocalName()) && URItoName.get(subject.getLocalName()) != null)
                sub = URItoName.get(subject.getLocalName());
            else
                sub = subject.getLocalName();
            if (URItoName.containsKey(predicate.getLocalName()) && URItoName.get(predicate.getLocalName()) != null)
                pred = URItoName.get(predicate.getLocalName());
            else
                pred = predicate.getLocalName();

            if (object.isLiteral()) {
                obj = object.asLiteral().getString();
            } else if (object.isResource()) {
                if (URItoName.containsKey(object.asResource().getLocalName()) && URItoName.get(object.asResource().getLocalName()) != null)
                    obj = URItoName.get(object.asResource().getLocalName());
                else
                    obj = object.asResource().getLocalName();
            } else
                obj = object.toString();
            System.out.println("subject is :" + sub);
            System.out.println("predicate is :" + pred);
            System.out.println("object is :" + obj);
            System.out.println("----------------------------------------------------");
            //Inferencing///////////////////////////////////////////
            if (pred.equals("type")) {
                if (!rev.containsKey(obj)) {
                    rev.put(obj, new ArrayList<String>());
                }
                rev.get(obj).add(sub);
            }
            if (pred.equals("type")) {
                domains.put(sub, obj);
            }
            if (!stp.containsKey(sub)) {
                stp.put(sub, new ArrayList<String>());
            }
            if (pred.equals("inverseOf")) {
                inverse.put(sub, obj);
            }
//            if(pred.equals("label")){
//                System.out.println("Inside inverse");
//
//            }
            if (!stp.get(sub).contains(pred)) {
                stp.get(sub).add(pred);
            }
 //           stp.get(sub).add(pred);
            List<String> list = new ArrayList<String>();
            list.add(sub);
            list.add(pred);
            if (!pto.containsKey(list)) {
                pto.put(list, new ArrayList<String>());
            }

            pto.get(list).add(obj);
            if (!PredTo.containsKey(pred)) {
                List<List<String>> temp = new ArrayList<List<String>>();
                List<String> subs = new ArrayList<String>();
                subs.add(sub);
                List<String> objs = new ArrayList<String>();
                objs.add(obj);
                temp.add(subs);
                temp.add(objs);
                PredTo.put(pred, temp);
            } else {
                PredTo.get(pred).get(0).add(sub);
                PredTo.get(pred).get(1).add(obj);
            }
            /////////// END Inferencing///////////////////////


            if (!TableColumn.containsKey(sub) && !ColumnTypes.containsKey(sub) && !pred.equals("label")) {
                if (pred.equals("type") && TableColumn.containsKey(obj))
                    tableItems.put(sub, obj);
                else {
                    if (objProperties.contains(pred)) {
                        HashMap<String, List<String>> maps;
                        if (objItems.containsKey(pred))
                            maps = objItems.get(pred);
                        else
                            maps = new HashMap<String, List<String>>();
                        List<String> maps2;
                        if (maps.containsKey(sub))
                            maps2 = maps.get(sub);
                        else
                            maps2 = new ArrayList<String>();
                        maps2.add(obj);
                        maps.put(sub, maps2);
                        objItems.put(pred, maps);
                    } else {
                        HashMap<String, String> map;
                        if (dataItems.containsKey(sub))
                            map = dataItems.get(sub);
                        else
                            map = new HashMap<String, String>();
                        map.put(pred, obj);
                        dataItems.put(sub, map);
                    }

                }
            }
        }
        //Inverse inferencing///
        System.out.println("Inverse Reasoning");
        for (Map.Entry mapElement : inverse.entrySet()) {
            String p = (String) mapElement.getKey();
            String k = (String) mapElement.getValue();
            System.out.println(p + "       " + k);
            List<String> subs = PredTo.get(k).get(0);
            List<String> objs = PredTo.get(k).get(1);
            System.out.println(subs);
            System.out.println(objs);
            for (int i = 0; i < objs.size(); i++) {
//                stp.put(item, p);
//                if(!stp.containsKey(objs.get(i))){
//                    stp.put(objs.get(i),new ArrayList<String>());
//                }
                if (!stp.get(objs.get(i)).contains(p)) {
                    stp.get(objs.get(i)).add(p);
                }
//                stp.get(objs.get(i)).add(p);
                List<String> trash = new ArrayList<String>();
                trash.add(objs.get(i));
                trash.add(p);
                if (!pto.containsKey(trash)) {
                    pto.put(trash, new ArrayList<String>());
                }
                pto.get(trash).add(subs.get(i));
                String sub= objs.get(i);
                String pred=p;
                String obj=subs.get(i);
                //Entering infered rules into db
                if (!TableColumn.containsKey(sub) && !ColumnTypes.containsKey(sub) && !pred.equals("label")) {
                    if (pred.equals("type") && TableColumn.containsKey(obj))
                        tableItems.put(sub, obj);
                    else {
                        if (objProperties.contains(pred)) {
                            HashMap<String, List<String>> maps;
                            if (objItems.containsKey(pred))
                                maps = objItems.get(pred);
                            else
                                maps = new HashMap<String, List<String>>();
                            List<String> maps2;
                            if (maps.containsKey(sub))
                                maps2 = maps.get(sub);
                            else
                                maps2 = new ArrayList<String>();
                            maps2.add(obj);
                            maps.put(sub, maps2);
                            objItems.put(pred, maps);
                        } else {
                            HashMap<String, String> map;
                            if (dataItems.containsKey(sub))
                                map = dataItems.get(sub);
                            else
                                map = new HashMap<String, String>();
                            map.put(pred, obj);
                            dataItems.put(sub, map);
                        }

                    }
                }
            }

        }

//        /////////////////////END IR//////////////////////////////////////////////////////
//
//        //Transitive Reasoning//////////////////////

        for (Map.Entry mapele : tp.entrySet()) {
            String p = (String) mapele.getKey();
            List<String> k = (List<String>) mapele.getValue();
            System.out.println(p);
            String domain = k.get(0);
            String range = k.get(1);

            List<String> dom = rev.get(domain);
            List<String> ran = rev.get(range);
            for (int i = 0; i < dom.size(); i++) {
                String temp = dom.get(i);
                for (int j = 0; j < objProperties.size(); j++) {
                    List<String> trash = new ArrayList<String>();
                    trash.add(temp);
                    trash.add(objProperties.get(j));
                    if (pto.containsKey(trash)) {
                        List<String> legal = pto.get(trash);
                        for (int l = 0; l < legal.size(); l++) {
                            System.out.println(legal.get(l) + "  " + temp);
                            for (int x = 0; x < objProperties.size(); x++) {
                                List<String> trash2 = new ArrayList<String>();
                                trash2.add(legal.get(l));
                                trash2.add(objProperties.get(x));
//                                    System.out.println(pto.get(trash2));
                                if (pto.containsKey(trash2)) {
                                    String check = pto.get(trash2).get(0);
//                                        System.out.println("Domain"+domains.get(check));
//                                        System.out.println("Rage"+ran);
                                    if (domains.get(check).equals(range)) {
//                                            System.out.println("inside");
                                        if (!stp.containsKey(temp)) {
                                            stp.put(temp, new ArrayList<String>());
                                        }

                                        if (!stp.get(temp).contains((p))) {
                                            stp.get(temp).add(p);
                                        }
                                        List<String> z = new ArrayList<String>();
                                        z.add(temp);
                                        z.add(p);

                                        List<String> add = pto.get(trash2);
                                        //SCJ
                                        pto.put(z, add);


                                        //////////////////////////Enter into db//////////////////////////////////////
                                        for(int o=0;o<add.size();o++) {
                                            String sub = temp;
                                            String pred = p;
                                            String obj =add.get(o);
                                            HashMap<String, List<String>> maps;
                                            if (objItems.containsKey(pred))
                                                maps = objItems.get(pred);
                                            else
                                                maps = new HashMap<String, List<String>>();
                                            List<String> maps2;
                                            if (maps.containsKey(sub))
                                                maps2 = maps.get(sub);
                                            else
                                                maps2 = new ArrayList<String>();
                                            maps2.add(obj);
                                            maps.put(sub, maps2);
                                            objItems.put(pred, maps);

                                        }
                                        ////////////////////////////Enter into Db ends////////////////////////////////////
                                    }
                                }
                            }
                        }


                    }
                }
            }


//            List<String>getpredsdom=stp.get()

    }
            ///////////////////////////////////////////// END TR/////////////////////////////////


            Iterator<Map.Entry<String, String>> iterator2 = tableItems.entrySet().iterator();
            while (iterator2.hasNext()) {
                String query = "INSERT INTO ";
                Map.Entry<String, String> next = iterator2.next();
                query += next.getValue() + "(" + next.getValue() + "_ID,";
                String query2 = "('" + next.getKey() + "',";
                try {
                    Iterator<Map.Entry<String, String>> iterator4 = dataItems.get(next.getKey()).entrySet().iterator();
                    while (iterator4.hasNext()) {
                        Map.Entry<String, String> next1 = iterator4.next();
                        query += next1.getKey() + ",";
                        if (ColumnTypes.get(next1.getKey()).equals("VARCHAR(30)"))
                            query2 += "'" + next1.getValue() + "' ,";
                        else
                            query2 += next1.getValue() + ",";
                    }
                    query = query.substring(0, query.length() - 1);
                    query += ") ";
                    query2 = query2.substring(0, query2.length() - 1);
                    query2 += ") ";
                    query = query + " VALUES " + query2;
                    System.out.println(query);
                    rdf.insertData(query);
                } catch (NullPointerException n) {
                    continue;
                }

            }

            Iterator<Map.Entry<String, HashMap<String, List<String>>>> iterator4 = objItems.entrySet().iterator();
            while (iterator4.hasNext()) {
                Map.Entry<String, HashMap<String, List<String>>> next = iterator4.next();
                Iterator<Map.Entry<String, List<String>>> iterator5 = next.getValue().entrySet().iterator();
                while (iterator5.hasNext()) {
                    Map.Entry<String, List<String>> next1 = iterator5.next();
                    Iterator<String> iterator6 = next1.getValue().iterator();
                    while (iterator6.hasNext()) {
                        String query = "INSERT INTO " + next.getKey() + " VALUES ('" + next1.getKey() + "', '" + iterator6.next() + "')";
                        System.out.println(query);
                        rdf.insertData(query);
                    }
                }
            }

            ///Inferencing loop/////////////////////////////////////
        int a=1;
        while(a==1) {
            System.out.println("select subject from following list:");
            for (Map.Entry mapElement : stp.entrySet()) {
                String key = (String) mapElement.getKey();
                System.out.println(key);

            }
            Scanner scan = new Scanner(System.in);
            System.out.print("Enter your subject: ");
            String name_s = scan.nextLine();

            List<String> list = new ArrayList<String>();

            list = stp.get(name_s);
            System.out.println("select predicate from following list");
            System.out.println(list);
            System.out.print("Enter your predicate: ");
            String name_p = scan.nextLine();
            List<String> list_o = new ArrayList<String>();

            List<String> temp = new ArrayList<String>();
            temp.add(name_s);
            temp.add(name_p);
            list_o = pto.get(temp);

            System.out.println(list_o);

            System.out.println("FINAL RESULT\n\n");
            System.out.println("With subject '" + name_s + "' predicate '" + name_p + "' object is => " + list_o + "\n\n");
            System.out.println(name_s + " " + name_p + " " + list_o+"\n\n");

            System.out.println(" To Continue press 1 ");
            a= scan.nextInt();
        }
            ///END infer loop/////////////////////////////////////////
        }
    }

