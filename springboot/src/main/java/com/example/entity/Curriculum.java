package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 课表信息
*/
@Data
public class Curriculum implements Serializable {
    private static final long serialVersionUID = 1L;

   private String segment;
   private String monday;
   private String tuesday;
   private String wednesday;
   private String thursday;
   private String friday;
   private String saturday;
   private String sunday;


}