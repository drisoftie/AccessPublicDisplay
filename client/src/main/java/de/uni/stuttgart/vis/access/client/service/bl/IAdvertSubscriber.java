package de.uni.stuttgart.vis.access.client.service.bl;

import android.bluetooth.le.ScanResult;

import java.util.List;

/**
 * @author Alexander Dridiger
 */
public interface IAdvertSubscriber {

    void onScanResultReceived(ScanResult result);

    void onScanResultsReceived(List<ScanResult> results);

    void onRefreshedScanReceived(ScanResult result);

    void onRefreshedScansReceived(List<ScanResult> results);

    void onScanLost(ScanResult lostResult);

    void onScanFailed(int errorCode);
}
