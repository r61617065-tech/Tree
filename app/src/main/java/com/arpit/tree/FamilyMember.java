package com.arpit.tree;

import java.util.ArrayList;
import java.util.List;

public class FamilyMember {
    public int id;
    public String name;
    public String gender;
    public Integer parent;
    public String alias;
    public String birth;
    public String death;
    public boolean deceased;
    public boolean star;
    public String note;
    public String husband;
    public String spouse;
    public Integer mother;
    public final List<String> wives = new ArrayList<>();
    public final List<Integer> children = new ArrayList<>();
}
