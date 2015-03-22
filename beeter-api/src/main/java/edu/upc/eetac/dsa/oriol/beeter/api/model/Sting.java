package edu.upc.eetac.dsa.oriol.beeter.api.model;

import java.util.List;

import javax.ws.rs.core.Link;

import org.glassfish.jersey.linking.Binding;
import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLink.Style;
import org.glassfish.jersey.linking.InjectLinks;

import edu.upc.eetac.dsa.oriol.beeter.api.MediaType;
import edu.upc.eetac.dsa.oriol.beeter.api.StingResource;

public class Sting {
	
	private int stingid;
	private String username;
	private String author;
	private String subject;
	private String content;
	private long lastModified;
	private long creationTimestamp;
	
	@InjectLinks({//Genera els links per anar a la col·lecció d'stings i també pot editar-se
@InjectLink(resource = StingResource.class, style = Style.ABSOLUTE, rel = "stings", title = "Latest stings", type = MediaType.BEETER_API_STING_COLLECTION),
@InjectLink(resource = StingResource.class, style = Style.ABSOLUTE, rel = "self edit", title = "Sting", type = MediaType.BEETER_API_STING, method = "getSting", bindings = @Binding(name = "stingid", value = "${instance.stingid}")) }) 
	
	private List<Link> links;	

	//getters and setters
	
	public int getStingid() {
		return stingid;
	}
	public void setStingid(int stringid) {
		this.stingid = stringid;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public long getLastModified() {
		return lastModified;
	}
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}
	public long getCreationTimestamp() {
		return creationTimestamp;
	}
	public void setCreationTimestamp(long creatonTimestamp) {
		this.creationTimestamp = creatonTimestamp;
	}

}
