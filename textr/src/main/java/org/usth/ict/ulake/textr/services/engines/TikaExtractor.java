package org.usth.ict.ulake.textr.services.engines;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped
public class TikaExtractor {
    
    private static final Logger log = LoggerFactory.getLogger(TikaExtractor.class);
    private static final Tika tika = new Tika();
    
    public String extractText(InputStream stream) throws IOException, TikaException {
        // TODO: should not extract to String by the way, could extract to Reader but the highlighter won't works
        log.info("Extracting text from {}", stream);
        
        return tika.parseToString(stream);
    }
}
