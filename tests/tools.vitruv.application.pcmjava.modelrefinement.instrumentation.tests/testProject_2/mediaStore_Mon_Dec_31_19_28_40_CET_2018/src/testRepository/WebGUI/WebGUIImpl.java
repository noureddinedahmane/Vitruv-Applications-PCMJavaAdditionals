package testRepository.WebGUI;

import testRepository.contracts.IWebGUI;
import testRepository.contracts.IMediaStore;

public class WebGUIImpl implements IWebGUI {private void internalMethod(){int i = 0;
		for(int j=0; j<10; j++) {
			i = i * j;
		}

}private IMediaStore iMediaStore;
	public void httpDownload () {
final int i = 5;
final int j = i + 1;for(int k = 0; k < 10; k++){
iMediaStore.download();
}
if(j<10){
iMediaStore.download();
}
internalMethod();	}

	public void httpUpload () {
	}
 }