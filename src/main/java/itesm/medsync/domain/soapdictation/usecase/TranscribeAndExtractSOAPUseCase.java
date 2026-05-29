package itesm.medsync.domain.soapdictation.usecase;

import itesm.medsync.domain.soapdictation.model.SOAPDictationResult;

public interface TranscribeAndExtractSOAPUseCase {
    SOAPDictationResult transcribeAndExtract(byte[] audioBytes, String mimeType);
}
