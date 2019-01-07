package test;

public class Model implements ModelInter{

	@Override
	public int summe(int i, int j) {
		final int k = 0;
		try {
			int s = i + j;
			return s;
		}
		finally {
			
		}
	}

	@Override
	public int multiplication(int i, int j) {
		try {
			int s = i + j;
			return s;
		}
		finally {
			
		}
		return i * j;
	}




}
