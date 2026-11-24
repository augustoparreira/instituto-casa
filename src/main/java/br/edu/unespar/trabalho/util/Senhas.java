package br.edu.unespar.trabalho.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2-HMAC-SHA256 nativo, salt individual e parâmetros junto ao hash. */
public final class Senhas {
    private static final int ITERACOES = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private Senhas() { }

    public static void validarNova(String senha) {
        if (senha == null || senha.length() < 12 || senha.length() > 128 || senha.isBlank())
            throw new IllegalArgumentException("A senha deve ter entre 12 e 128 caracteres.");
    }

    public static String gerarHash(String senha) {
        if (senha == null || senha.length() > 1024) throw new IllegalArgumentException("Senha inválida.");
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "pbkdf2-sha256$" + ITERACOES + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derivar(senha, salt, ITERACOES));
    }

    public static boolean verificar(String senha, String hash) {
        if (senha == null || hash == null || senha.length() > 1024) return false;
        try {
            String[] partes = hash.split("\\$");
            if (partes.length != 4 || !partes[0].equals("pbkdf2-sha256")) return false;
            int iteracoes = Integer.parseInt(partes[1]);
            if (iteracoes < ITERACOES || iteracoes > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            if (salt.length != 16 || esperado.length != 32) return false;
            byte[] atual = derivar(senha, salt, iteracoes);
            try { return MessageDigest.isEqual(esperado, atual); }
            finally { Arrays.fill(atual, (byte) 0); }
        } catch (IllegalArgumentException e) { return false; }
    }

    private static byte[] derivar(String senha, byte[] salt, int iteracoes) {
        char[] caracteres = senha.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(caracteres, salt, iteracoes, 256);
        Arrays.fill(caracteres, '\0');
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException("Proteção de senha indisponível."); }
        finally { spec.clearPassword(); }
    }
}
