package br.edu.unespar.trabalho.model;

public class Responsavel extends Pessoa {
    private String parentesco;
    private boolean contatoPrincipal;

    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }
    public boolean isContatoPrincipal() { return contatoPrincipal; }
    public void setContatoPrincipal(boolean contatoPrincipal) { this.contatoPrincipal = contatoPrincipal; }
}