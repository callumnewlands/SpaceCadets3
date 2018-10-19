import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamReader extends InputStreamReader
{
    StreamReader(InputStream in)
    {
        super(in);
    }

    @Override
    public void close() throws IOException {} // Ensures that System.In is not closed when resources are tidied up
}