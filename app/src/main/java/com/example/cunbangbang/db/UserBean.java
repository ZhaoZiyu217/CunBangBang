package com.example.cunbangbang.db;

import java.io.Serializable;

public class UserBean implements Serializable {
    private int id;
    private String name;
    private String village;
    private String role;
    private int points;

    public UserBean() {}

    public UserBean(int id, String name, String village, String role, int points) {
        this.id = id;
        this.name = name;
        this.village = village;
        this.role = role;
        this.points = points;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
}