/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc., and individual contributors as indicated
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


package org.jboss.web.rewrite;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;

public class Substitution {

    public abstract class SubstitutionElement {
        public abstract String evaluate(Matcher rule, Matcher cond, Resolver resolver);
    }
    
    public class StaticElement extends SubstitutionElement {
        public String value;

        public String evaluate
            (Matcher rule, Matcher cond, Resolver resolver) {
            return value;
        }
    
    }
    
    public class RewriteRuleBackReferenceElement extends SubstitutionElement {
        public int n;
        public String evaluate(Matcher rule, Matcher cond, Resolver resolver) {
            return rule.group(n);
        }
    }
    
    public class RewriteCondBackReferenceElement extends SubstitutionElement {
        public int n;
        public String evaluate(Matcher rule, Matcher cond, Resolver resolver) {
            return cond.group(n);
        }
    }
    
    public class ServerVariableElement extends SubstitutionElement {
        public String key;
        public String evaluate(Matcher rule, Matcher cond, Resolver resolver) {
            return resolver.resolve(key);
        }
    }
    
    public class ServerVariableEnvElement extends SubstitutionElement {
        public String key;
        public String evaluate(Matcher rule, Matcher cond, Resolver resolver) {
            return resolver.resolveEnv(key);
        }
    }
    
    public class ServerVariableSslElement extends SubstitutionElement {
        public String key;
        public String evaluate(Matcher rule, Matcher cond, Resolver resolver) {
            return resolver.resolveSsl(key);
        }
    }
    
    public class ServerVariableHttpElement extends SubstitutionElement {
        public String key;
        public String evaluate(Matcher rule, Matcher cond, Resolver resolver) {
            return resolver.resolveHttp(key);
        }
    }
    
    public class MapElement extends SubstitutionElement {
        public RewriteMap map = null;
        public String key;
        public String defaultValue = null;
        public String evaluate(Matcher rule, Matcher cond, Resolver resolver) {
            String result = map.lookup(key);
            if (result == null) {
                result = defaultValue;
            }
            return result;
        }
    }
    
    protected SubstitutionElement[] elements = null;

    protected String sub = null;
    public String getSub() { return sub; }
    public void setSub(String sub) { this.sub = sub; }

    public void parse(Map maps) {

        ArrayList elements = new ArrayList();
        int pos = 0;
        int percentPos = 0;
        int dollarPos = 0;
        
        while (pos < sub.length()) {
            percentPos = sub.indexOf('%', pos);
            dollarPos = sub.indexOf('$', pos);
            // FIXME: System.out.println("S: " + sub + " pos: " + pos + " L: " + sub.length() + " %: " + percentPos + " $: " + dollarPos);
            if (percentPos == -1 && dollarPos == -1) {
                // Static text
                StaticElement newElement = new StaticElement();
                newElement.value = sub.substring(pos, sub.length());
                pos = sub.length();
                elements.add(newElement);
            } else if (percentPos == -1 || ((dollarPos != -1) && (dollarPos < percentPos))) {
                // $: backreference to rule or map lookup
                if (dollarPos + 1 == sub.length()) {
                    throw new IllegalArgumentException(sub);
                }
                if (pos < dollarPos) {
                    // Static text
                    StaticElement newElement = new StaticElement();
                    newElement.value = sub.substring(pos, dollarPos);
                    pos = dollarPos;
                    elements.add(newElement);
                }
                if (Character.isDigit(sub.charAt(dollarPos + 1))) {
                    // $: backreference to rule
                    RewriteRuleBackReferenceElement newElement = new RewriteRuleBackReferenceElement();
                    newElement.n = Character.digit(sub.charAt(dollarPos + 1), 10);
                    pos = dollarPos + 2;
                    elements.add(newElement);
                } else {
                    // $: map lookup as ${mapname:key|default}
                    MapElement newElement = new MapElement();
                    int open = sub.indexOf('{', dollarPos);
                    int colon = sub.indexOf(':', dollarPos);
                    int def = sub.indexOf('|', dollarPos);
                    int close = sub.indexOf('}', dollarPos);
                    if (!(-1 < open && open < colon && colon < close)) {
                        throw new IllegalArgumentException(sub);
                    }
                    newElement.map = (RewriteMap) maps.get(sub.substring(open + 1, colon));
                    if (newElement.map == null) {
                        throw new IllegalArgumentException(sub + ": No map: " + sub.substring(open + 1, colon));
                    }
                    if (def > -1) {
                        if (!(colon < def && def < close)) {
                            throw new IllegalArgumentException(sub);
                        }
                        newElement.key = sub.substring(colon + 1, def);
                        newElement.defaultValue = sub.substring(def + 1, close);
                    } else {
                        newElement.key = sub.substring(colon + 1, close);
                    }
                    pos = close + 1;
                    elements.add(newElement);
                }
            } else {
                // %: backreference to cond or server variable
                if (percentPos + 1 == sub.length()) {
                    throw new IllegalArgumentException(sub);
                }
                if (pos < percentPos) {
                    // Static text
                    StaticElement newElement = new StaticElement();
                    newElement.value = sub.substring(pos, percentPos);
                    pos = percentPos;
                    elements.add(newElement);
                }
                if (Character.isDigit(sub.charAt(percentPos + 1))) {
                    // %: backreference to cond
                    RewriteCondBackReferenceElement newElement = new RewriteCondBackReferenceElement();
                    newElement.n = Character.digit(sub.charAt(percentPos + 1), 10);
                    pos = percentPos + 2;
                    elements.add(newElement);
                } else {
                    // %: server variable as %{variable}
                    SubstitutionElement newElement = null;
                    int open = sub.indexOf('{', percentPos);
                    int colon = sub.indexOf(':', percentPos);
                    int close = sub.indexOf('}', percentPos);
                    if (!(-1 < open && open < close)) {
                        throw new IllegalArgumentException(sub);
                    }
                    if (colon > -1) {
                        if (!(open < colon && colon < close)) {
                            throw new IllegalArgumentException(sub);
                        }
                        String type = sub.substring(open + 1, colon);
                        if (type.equals("ENV")) {
                            newElement = new ServerVariableEnvElement();
                            ((ServerVariableEnvElement) newElement).key = sub.substring(colon + 1, close);
                        } else if (type.equals("SSL")) {
                            newElement = new ServerVariableSslElement();
                            ((ServerVariableEnvElement) newElement).key = sub.substring(colon + 1, close);
                        } else if (type.equals("HTTP")) {
                            newElement = new ServerVariableHttpElement();
                            ((ServerVariableEnvElement) newElement).key = sub.substring(colon + 1, close);
                        } else {
                            throw new IllegalArgumentException(sub + ": Bad type: " + type);
                        }
                    } else {
                        newElement = new ServerVariableElement();
                        ((ServerVariableElement) newElement).key = sub.substring(open + 1, close);
                    }
                    pos = close + 1;
                    elements.add(newElement);
                }
            }
        }
        
        this.elements = (SubstitutionElement[]) elements.toArray(new SubstitutionElement[0]);
        
    }
    
    /**
     * Create a substitution with the given string.
     */
    /*
    public Substitution(String sub, Map maps) {
        ArrayList elements = new ArrayList();
        int pos = 0;
        int percentPos = 0;
        int dollarPos = 0;
        
        while (pos < sub.length()) {
            percentPos = sub.indexOf('%', pos);
            dollarPos = sub.indexOf('$', pos);
            // FIXME: System.out.println("S: " + sub + " pos: " + pos + " L: " + sub.length() + " %: " + percentPos + " $: " + dollarPos);
            if (percentPos == -1 && dollarPos == -1) {
                // Static text
                StaticElement newElement = new StaticElement();
                newElement.value = sub.substring(pos, sub.length());
                pos = sub.length();
                elements.add(newElement);
            } else if (percentPos == -1 || ((dollarPos != -1) && (dollarPos < percentPos))) {
                // $: backreference to rule or map lookup
                if (dollarPos + 1 == sub.length()) {
                    throw new IllegalArgumentException(sub);
                }
                if (pos < dollarPos) {
                    // Static text
                    StaticElement newElement = new StaticElement();
                    newElement.value = sub.substring(pos, dollarPos);
                    pos = dollarPos;
                    elements.add(newElement);
                }
                if (Character.isDigit(sub.charAt(dollarPos + 1))) {
                    // $: backreference to rule
                    RewriteRuleBackReferenceElement newElement = new RewriteRuleBackReferenceElement();
                    newElement.n = Character.digit(sub.charAt(dollarPos + 1), 10);
                    pos = dollarPos + 2;
                    elements.add(newElement);
                } else {
                    // $: map lookup as ${mapname:key|default}
                    MapElement newElement = new MapElement();
                    int open = sub.indexOf('{', dollarPos);
                    int colon = sub.indexOf(':', dollarPos);
                    int def = sub.indexOf('|', dollarPos);
                    int close = sub.indexOf('}', dollarPos);
                    if (!(-1 < open && open < colon && colon < close)) {
                        throw new IllegalArgumentException(sub);
                    }
                    newElement.map = (RewriteMap) maps.get(sub.substring(open + 1, colon));
                    if (newElement.map == null) {
                        throw new IllegalArgumentException(sub + ": No map: " + sub.substring(open + 1, colon));
                    }
                    if (def > -1) {
                        if (!(colon < def && def < close)) {
                            throw new IllegalArgumentException(sub);
                        }
                        newElement.key = sub.substring(colon + 1, def);
                        newElement.defaultValue = sub.substring(def + 1, close);
                    } else {
                        newElement.key = sub.substring(colon + 1, close);
                    }
                    pos = close + 1;
                    elements.add(newElement);
                }
            } else {
                // %: backreference to cond or server variable
                if (percentPos + 1 == sub.length()) {
                    throw new IllegalArgumentException(sub);
                }
                if (pos < percentPos) {
                    // Static text
                    StaticElement newElement = new StaticElement();
                    newElement.value = sub.substring(pos, percentPos);
                    pos = percentPos;
                    elements.add(newElement);
                }
                if (Character.isDigit(sub.charAt(percentPos + 1))) {
                    // %: backreference to cond
                    RewriteCondBackReferenceElement newElement = new RewriteCondBackReferenceElement();
                    newElement.n = Character.digit(sub.charAt(percentPos + 1), 10);
                    pos = percentPos + 2;
                    elements.add(newElement);
                } else {
                    // %: server variable as %{variable}
                    SubstitutionElement newElement = null;
                    int open = sub.indexOf('{', percentPos);
                    int colon = sub.indexOf(':', percentPos);
                    int close = sub.indexOf('}', percentPos);
                    if (!(-1 < open && open < close)) {
                        throw new IllegalArgumentException(sub);
                    }
                    if (colon > -1) {
                        if (!(open < colon && colon < close)) {
                            throw new IllegalArgumentException(sub);
                        }
                        String type = sub.substring(open + 1, colon);
                        if (type.equals("ENV")) {
                            newElement = new ServerVariableEnvElement();
                            ((ServerVariableEnvElement) newElement).key = sub.substring(colon + 1, close);
                        } else if (type.equals("SSL")) {
                            newElement = new ServerVariableSslElement();
                            ((ServerVariableEnvElement) newElement).key = sub.substring(colon + 1, close);
                        } else if (type.equals("HTTP")) {
                            newElement = new ServerVariableHttpElement();
                            ((ServerVariableEnvElement) newElement).key = sub.substring(colon + 1, close);
                        } else {
                            throw new IllegalArgumentException(sub + ": Bad type: " + type);
                        }
                    } else {
                        newElement = new ServerVariableElement();
                        ((ServerVariableElement) newElement).key = sub.substring(open + 1, close);
                    }
                    pos = close + 1;
                    elements.add(newElement);
                }
            }
        }
        
        this.elements = (SubstitutionElement[]) elements.toArray(new SubstitutionElement[0]);
        
    }
    */
    
    /**
     * Evaluate the substituation based on the context
     * 
     * @param rule corresponding matched rule
     * @param cond last matched condition
     * @return
     */
    public String evaluate(Matcher rule, Matcher cond, Resolver resolver) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < elements.length; i++) {
            buf.append(elements[i].evaluate(rule, cond, resolver));
        }
        return buf.toString();
    }

}
