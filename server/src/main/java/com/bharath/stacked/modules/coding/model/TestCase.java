package com.bharath.stacked.modules.coding.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TestCase {
    private String id;

    //stdin for normal/generic programming languages
    private String stdin;

    //init script for the database languages
    private String dbInit;

    //output for the testcases
    private String output;
}
