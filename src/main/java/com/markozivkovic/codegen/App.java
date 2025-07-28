package com.markozivkovic.codegen;

import java.util.List;

import com.markozivkovic.codegen.model.FieldDefinition;
import com.markozivkovic.codegen.model.ModelDefinition;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        FieldDefinition id = new FieldDefinition();
        id.setName("id");
        id.setType("Long");
        id.setId(true);

        FieldDefinition name = new FieldDefinition();
        name.setName("name");
        name.setType("String");

        ModelDefinition user = new ModelDefinition();
        user.setName("UserModel");
        user.setTableName("users");
        user.setFields(List.of(id, name));

        SpringCrudGenerator generator = new SpringCrudGenerator();
        generator.generate(user);
    }
}
