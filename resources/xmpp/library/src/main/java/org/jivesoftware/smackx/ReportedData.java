/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jivesoftware.smackx;

import java.util.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.packet.DataForm;

/**
 * Represents a set of data results returned as part of a search. The report is structured 
 * in columns and rows.
 * 
 * @author Gaston Dombiak
 */
public class ReportedData {
    
    private List columns = new ArrayList();
    private List rows = new ArrayList();
    private String title = "";
    
    /**
     * Returns a new ReportedData if the packet is used for reporting data and includes an 
     * extension that matches the elementName and namespace "x","jabber:x:data".
     * 
     * @param packet the packet used for reporting data.
     */
    public static ReportedData getReportedDataFrom(Packet packet) {
        // Check if the packet includes the DataForm extension
        PacketExtension packetExtension = packet.getExtension("x","jabber:x:data");
        if (packetExtension != null) {
            // Check if the existing DataForm is a result of a search
            DataForm dataForm = (DataForm) packetExtension;
            if (dataForm.getReportedData() != null)
                return new ReportedData(dataForm);
        }
        // Otherwise return null
        return null;
    }


    /**
     * Creates a new ReportedData based on the returned dataForm from a search
     *(namespace "jabber:iq:search").
     *
     * @param dataForm the dataForm returned from a search (namespace "jabber:iq:search").
     */
    private ReportedData(DataForm dataForm) {
        // Add the columns to the report based on the reported data fields
        for (Iterator fields = dataForm.getReportedData().getFields(); fields.hasNext();) {
            FormField field = (FormField)fields.next();
            columns.add(new Column(field.getLabel(), field.getVariable(), field.getType()));
        }

        // Add the rows to the report based on the form's items
        for (Iterator items = dataForm.getItems(); items.hasNext();) {
            DataForm.Item item = (DataForm.Item)items.next();
            List fieldList = new ArrayList(columns.size());
            FormField field;
            for (Iterator fields = item.getFields(); fields.hasNext();) {
                field = (FormField) fields.next();
                // The field is created with all the values of the data form's field
                List values = new ArrayList();
                for (Iterator it=field.getValues(); it.hasNext();) {
                    values.add(it.next());
                }
                fieldList.add(new Field(field.getVariable(), values));
            }
            rows.add(new Row(fieldList));
        }

        // Set the report's title
        this.title = dataForm.getTitle();
    }


    public ReportedData(){
        // Allow for model creation of ReportedData.
    }

    /**
     * Adds a new <code>Row</code>.
     * @param row the new row to add.
     */
    public void addRow(Row row){
        rows.add(row);
    }

    /**
     * Adds a new <code>Column</code>
     * @param column the column to add.
     */
    public void addColumn(Column column){
        columns.add(column);
    }


    /**
     * Returns an Iterator for the rows returned from a search.
     *
     * @return an Iterator for the rows returned from a search.
     */
    public Iterator getRows() {
        return Collections.unmodifiableList(new ArrayList(rows)).iterator();
    }

    /**
     * Returns an Iterator for the columns returned from a search.
     *
     * @return an Iterator for the columns returned from a search.
     */
    public Iterator getColumns() {
        return Collections.unmodifiableList(new ArrayList(columns)).iterator();
    }


    /**
     * Returns the report's title. It is similar to the title on a web page or an X
     * window.
     *
     * @return title of the report.
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     * Represents the columns definition of the reported data.
     *
     * @author Gaston Dombiak
     */
    public static class Column {
        private String label;
        private String variable;
        private String type;

        /**
         * Creates a new column with the specified definition.
         *
         * @param label the columns's label.
         * @param variable the variable name of the column.
         * @param type the format for the returned data.
         */
        public Column(String label, String variable, String type) {
            this.label = label;
            this.variable = variable;
            this.type = type;
        }

        /**
         * Returns the column's label.
         *
         * @return label of the column.
         */
        public String getLabel() {
            return label;
        }


        /**
         * Returns the column's data format. Valid formats are:
         *
         * <ul>
         *  <li>text-single -> single line or word of text
         *  <li>text-private -> instead of showing the user what they typed, you show ***** to
         * protect it
         *  <li>text-multi -> multiple lines of text entry
         *  <li>list-single -> given a list of choices, pick one
         *  <li>list-multi -> given a list of choices, pick one or more
         *  <li>boolean -> 0 or 1, true or false, yes or no. Default value is 0
         *  <li>fixed -> fixed for putting in text to show sections, or just advertise your web
         * site in the middle of the form
         *  <li>hidden -> is not given to the user at all, but returned with the questionnaire
         *  <li>jid-single -> Jabber ID - choosing a JID from your roster, and entering one based
         * on the rules for a JID.
         *  <li>jid-multi -> multiple entries for JIDs
         * </ul>
         *
         * @return format for the returned data.
         */
        public String getType() {
            return type;
        }


        /**
         * Returns the variable name that the column is showing.
         *
         * @return the variable name of the column.
         */
        public String getVariable() {
            return variable;
        }


    }

    public static class Row {
        private List fields = new ArrayList();

        public Row(List fields) {
            this.fields = fields;
        }

        /**
         * Returns the values of the field whose variable matches the requested variable.
         *
         * @param variable the variable to match.
         * @return the values of the field whose variable matches the requested variable.
         */
        public Iterator getValues(String variable) {
            for(Iterator it=getFields();it.hasNext();) {
                Field field = (Field) it.next();
                if (variable.equalsIgnoreCase(field.getVariable())) {
                    return field.getValues();
                }
            }
            return null;
        }

        /**
         * Returns the fields that define the data that goes with the item.
         *
         * @return the fields that define the data that goes with the item.
         */
        private Iterator getFields() {
            return Collections.unmodifiableList(new ArrayList(fields)).iterator();
        }
    }

    public static class Field {
        private String variable;
        private List values;

        public Field(String variable, List values) {
            this.variable = variable;
            this.values = values;
        }

        /**
         * Returns the variable name that the field represents.
         * 
         * @return the variable name of the field.
         */
        public String getVariable() {
            return variable;
        }

        /**
         * Returns an iterator on the values reported as part of the search.
         * 
         * @return the returned values of the search.
         */
        public Iterator getValues() {
            return Collections.unmodifiableList(values).iterator();
        }
    }
}
