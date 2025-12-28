package com.extractor.unraveldocs.tools;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Comparator;
import java.util.List;

public class FieldSorter {
    static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java FieldSorter <JavaFile>");
            return;
        }
        File file = new File(args[0]);
        CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(file));

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            List<FieldDeclaration> fields = clazz.getMembers().stream()
                    .filter(m -> m instanceof FieldDeclaration)
                    .map(m -> (FieldDeclaration) m).sorted(Comparator.comparing(f -> f.getVariables().getFirst().toString())).toList();

            fields.forEach(clazz::remove);

            for (int i = fields.size() - 1; i >= 0; i--) {
                clazz.getMembers().addFirst(fields.get(i));
            }
        });

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(cu.toString());
        }
        System.out.println("Fields sorted in " + file.getName());
    }
}