package edu.utn.frsf.isi.dan.user.dto;

public record GoogleAuthResponse(
    Integer userId, String email, String nombre, String givenName, String familyName) {}

