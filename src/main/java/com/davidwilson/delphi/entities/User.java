package com.davidwilson.delphi.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "users", schema = "public", catalog = "delphi_dev")
public class User {
    private Integer id;
    private String user_id;
    private String pfp;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Basic
    @Column(name = "user_id")
    public String getUserId() {
        return user_id;
    }

    public void setUserId(String userId) {
        this.user_id = userId;
    }

    @Basic
    @Column(name = "pfp")
    public String getPfp() {
        return pfp;
    }

    public void setPfp(String pfp) {
        this.pfp = pfp;
    }
}