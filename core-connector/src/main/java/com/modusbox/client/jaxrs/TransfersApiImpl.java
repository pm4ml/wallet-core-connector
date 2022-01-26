package com.modusbox.client.jaxrs;

import com.modusbox.client.api.TransfersApi;
import com.modusbox.client.model.FulfilNotification;
import com.modusbox.client.model.TransferStatusResponse;
import com.modusbox.client.model.TransferRequestInbound;
import com.modusbox.client.model.TransferResponseInbound;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

public class TransfersApiImpl implements TransfersApi {

    @Override
    public TransferStatusResponse getTransfersByTransferId(@Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$") String transferId) {
        return null;
    }

    @Override
    public TransferResponseInbound postTransfers(TransferRequestInbound transferRequestInbound) {
        return null;
    }

    @Override
    public TransferResponseInbound putTransfersByTransferId(@Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$") String transferId, @Valid FulfilNotification fulfilNotification) {
        return null;
    }

//    @Override
//    public void putTransfersByTransferId(@Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$") String transferId, @Valid FulfilNotification fulfilNotification) {
//        // No need to return null as method is void
//    }
}
