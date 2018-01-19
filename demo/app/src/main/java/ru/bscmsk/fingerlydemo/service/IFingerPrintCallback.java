package ru.bscmsk.fingerlydemo.service;

import javax.crypto.Cipher;

/**
 * Created by mlakatkin on 06.06.2017.
 */

public interface IFingerPrintCallback {

	//The finger is recognized
	void onFingerRecognized(Cipher cipher);

	//Finger could not be recognized
	void onFingerIncorrect();

	//the system has locked the scanner. The method is called every second, epsTime countdown ms before unlocking
	//When epsTime becomes 0, you can try to access the scanner again
	void onTicketFingerLocked(int epsTime);

	//the system has locked the scanner. The method is called every second, epsTime countdown ms before unlocking
	//When epsTime becomes 0, you can try to access the scanner again
	void onFinishFingerLocked();

	// Scanning canceled.
	void onCancelled();

	void onHelpResult(int helpMsgId);
}
