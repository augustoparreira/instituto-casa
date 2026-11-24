package br.edu.unespar.trabalho.util;

import br.edu.unespar.trabalho.dao.EquipeTecnicaDAO;
import br.edu.unespar.trabalho.model.*;
import java.io.Console;
import java.time.LocalDate;
import java.util.Arrays;

/** Ferramenta local de instalação. Exige credenciais externas do PostgreSQL e console real. */
public final class AdministracaoInicial {
    public static void main(String[] args) {
        Console console=System.console();
        if(console==null) throw new IllegalStateException("Execute em um terminal interativo; não informe senhas por argumentos.");
        if(args.length!=1) { console.printf("Use migrar-senhas ou criar-administrador.%n"); return; }
        if(!"CONFIRMAR".equals(console.readLine("Confira o banco configurado e o backup. Digite CONFIRMAR para continuar: "))) return;
        try {
            EquipeTecnicaDAO dao=new EquipeTecnicaDAO();
            switch(args[0]) {
                case "migrar-senhas" -> console.printf("Contas convertidas: %d%n",dao.migrarSenhasLegadas());
                case "criar-administrador" -> {
                    EquipeTecnica m=new EquipeTecnica();
                    m.setCpf(Long.parseLong(console.readLine("CPF (somente números): ")));
                    m.setNomeCompleto(console.readLine("Nome: "));
                    m.setDataNascimento(LocalDate.parse(console.readLine("Nascimento (AAAA-MM-DD): ")));
                    m.setCargoFuncao(console.readLine("Cargo: ")); m.setLogin(console.readLine("Login: "));
                    m.setNivelAcesso(NivelAcesso.ADMINISTRADOR);
                    char[] senha=console.readPassword("Senha (12 a 128 caracteres): ");
                    char[] confirmar=console.readPassword("Confirme a senha: ");
                    try {
                        if(senha==null || confirmar==null || !Arrays.equals(senha,confirmar)) throw new IllegalArgumentException("Senhas diferentes.");
                        m.setSenha(new String(senha)); dao.criarPrimeiroAdministrador(m);
                        console.printf("Administrador criado. Faça login pelo aplicativo.%n");
                    } finally { if(senha!=null) Arrays.fill(senha,'\0'); if(confirmar!=null) Arrays.fill(confirmar,'\0'); m.setSenha(null); }
                }
                default -> console.printf("Comando desconhecido.%n");
            }
        } catch(RuntimeException e) { console.printf("Operação não concluída. Confira dados, configuração e migração; nenhuma credencial será exibida.%n"); }
    }
}
