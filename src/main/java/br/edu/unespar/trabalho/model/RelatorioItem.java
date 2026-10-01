package br.edu.unespar.trabalho.model;

public class RelatorioItem {
    private String titulo;
    private String categoria;
    private String parecer;
    private String data;

    public RelatorioItem(String titulo, String categoria, String parecer, String data) {
        this.titulo = titulo;
        this.categoria = categoria;
        this.parecer = parecer;
        this.data = data;
    }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getParecer() { return parecer; }
    public void setParecer(String parecer) { this.parecer = parecer; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    @Override
    public String toString() { return titulo; }
}