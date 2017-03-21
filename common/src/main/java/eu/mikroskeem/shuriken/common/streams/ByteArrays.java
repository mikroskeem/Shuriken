package eu.mikroskeem.shuriken.common.streams;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static eu.mikroskeem.shuriken.common.Ensure.notNull;

public class ByteArrays {
    /**
     * Convert an {@link InputStream} to byte array
     *
     * @param inputStream Source {@link InputStream}
     * @return byte array or null, if reading failed
     */
    @Nullable
    @Contract("null -> fail; !null -> !null")
    public static byte[] fromInputStream(InputStream inputStream) {
        notNull(inputStream, "Input stream must not be null!");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            int length;
            while ((length =inputStream.read(buffer)) != -1)
                output.write(buffer, 0, length);
            return output.toByteArray();
        } catch (IOException e){
            return null;
        } finally {
            try {
                inputStream.close();
                output.close();
            }
            catch (IOException ignored){}
        }
    }
}
