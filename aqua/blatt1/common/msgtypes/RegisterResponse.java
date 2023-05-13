package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;
	private final int lease_dur;
	private final boolean re;


	public RegisterResponse(int lease_dur, String id, boolean re) {
		this.lease_dur = lease_dur;
		this.id = id;
		this.re = re;
	}

	public String getId() {
		return id;
	}

	public int getLeaseDur() {return lease_dur;}

	public boolean getRe() {return re;}

}
