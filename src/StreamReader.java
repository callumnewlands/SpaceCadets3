import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * An InputStreamReader that ensures that System.In is not closed when resources are tidied up
 */
public class StreamReader extends InputStreamReader
{
    StreamReader(InputStream in)
    {
        super(in);
    }

    @Override
    public void close() throws IOException {} // Ensures that System.In is not closed when resources are tidied up
}