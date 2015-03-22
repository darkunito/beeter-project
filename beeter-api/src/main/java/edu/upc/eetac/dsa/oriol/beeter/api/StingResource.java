package edu.upc.eetac.dsa.oriol.beeter.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.sql.DataSource;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import edu.upc.eetac.dsa.oriol.beeter.api.model.Sting;
import edu.upc.eetac.dsa.oriol.beeter.api.model.StingCollection;

@Path("/stings")
public class StingResource {
	private DataSource ds = DataSourceSPA.getInstance().getDataSource();

	@Context
	private SecurityContext security;
	// método getStings()

	// private String GET_STINGS_QUERY =
	// "select s.*, u.name from stings s, users u where u.username=s.username order by creation_timestamp desc";
	// equival a:
	// select users.name, stings.* from users, stings where
	// users.username=stings.username order by creation_timestamp desc;

	private String GET_STINGS_QUERY = "select s.*, u.name from stings s, users u where u.username=s.username and s.creation_timestamp < ifnull(?, now()) order by creation_timestamp desc limit ?";
	private String GET_STINGS_QUERY_FROM_LAST = "select s.*, u.name from stings s, users u where u.username=s.username and s.creation_timestamp > ? order by creation_timestamp desc";

	@GET
	@Produces(MediaType.BEETER_API_STING_COLLECTION)
	public StingCollection getStings(@QueryParam("length") int length,
									 @QueryParam("before") long before, 
									 @QueryParam("after") long after) {
		
		StingCollection stings = new StingCollection();

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			boolean updateFromLast = after > 0;
			stmt = updateFromLast ? conn
					.prepareStatement(GET_STINGS_QUERY_FROM_LAST) : conn
					.prepareStatement(GET_STINGS_QUERY);
			if (updateFromLast) {
				stmt.setTimestamp(1, new Timestamp(after));
			} else {
				if (before > 0)
					stmt.setTimestamp(1, new Timestamp(before));
				else
					stmt.setTimestamp(1, null);
				length = (length <= 0) ? 5 : length;// Si no pasas lenght, por
													// defecto será 5
				stmt.setInt(2, length);
			}
			ResultSet rs = stmt.executeQuery();
			boolean first = true;
			long oldestTimestamp = 0;
			while (rs.next()) {
				Sting sting = new Sting();
				sting.setStingid(rs.getInt("stingid"));
				sting.setUsername(rs.getString("username"));
				sting.setAuthor(rs.getString("name"));
				sting.setSubject(rs.getString("subject"));
				sting.setLastModified(rs.getTimestamp("last_modified")
						.getTime());
				sting.setCreationTimestamp(rs
						.getTimestamp("creation_timestamp").getTime());
				oldestTimestamp = rs.getTimestamp("creation_timestamp")
						.getTime();
				sting.setLastModified(oldestTimestamp);
				if (first) {
					first = false;
					stings.setNewestTimestamp(sting.getCreationTimestamp());
				}
				stings.addSting(sting);
			}
			stings.setOldestTimestamp(oldestTimestamp);
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}

		return stings;
	}

	// ////////////////////
	// Método getSting cacheable
	private String GET_STING_BY_ID_QUERY = "select s.*, u.name from stings s, users u where u.username=s.username and s.stingid=?";

	@GET
	@Path("/{stingid}")
	@Produces(MediaType.BEETER_API_STING)
	public Response getSting(@PathParam("stingid") String stingid,
			@Context Request request) {
		// Create CacheControl
		CacheControl cc = new CacheControl();

		Sting sting = getStingFromDatabase(stingid);

		// Calculate the ETag on last modified date of user resource
		EntityTag eTag = new EntityTag(Long.toString(sting.getLastModified()));

		// Verify if it matched with etag available in http request
		Response.ResponseBuilder rb = request.evaluatePreconditions(eTag);

		// If ETag matches the rb will be non-null;
		// Use the rb to return the response without any further processing
		if (rb != null) {
			return rb.cacheControl(cc).tag(eTag).build();
		}

		// If rb is null then either it is first time request; or resource is
		// modified
		// Get the updated representation and return with Etag attached to it
		rb = Response.ok(sting).cacheControl(cc).tag(eTag);

		return rb.build();
	}

	// ////////////////////////////////////////////////////////
	// Ejercicio 1: Obtener stings por subject y content, con un máximo de
	// resultados (lenght)
	private String GET_STINGS_QUERY_BY_SUBJECT_AND_CONTENT = "select users.name, stings.* from users, stings where users.username=stings.username and subject like ? and content like ? order by creation_timestamp desc";

	@GET
	@Path("search")
	@Produces(MediaType.BEETER_API_STING_COLLECTION)
	public StingCollection getStings(@QueryParam("length") int length,
			@QueryParam("subject") String subject,
			@QueryParam("content") String content) {

		StingCollection stings = new StingCollection();

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;

		try { // Preparem la Query
			if (subject != null && content != null) {
				stmt = conn
						.prepareStatement(GET_STINGS_QUERY_BY_SUBJECT_AND_CONTENT);

				// Añadimos los dos ? de la consulta
				String subj = "%" + subject + "%";
				stmt.setString(1, subj);// Pasamos el valor de sujb a la
										// consulta

				String cont = "%" + content + "%";
				stmt.setString(2, cont);// Pasamos el valor de cont a la
										// consulta

				length = (length <= 0) ? 5 : length; // length será 5 si se pone
														// un valor negativo,
														// sino será el valor
														// que le pase el
														// usuario
				stmt.setInt(3, length);
			} else {
				throw new BadRequestException(
						"No has indicado el subject o el content del sting.");
			}// Query preparada

			ResultSet rs = stmt.executeQuery(); // Enviem Query a base de dades
			boolean first = true;
			long oldestTimestamp = 0;

			while (rs.next()) { // Afegim tots els stings que ens retorni la
								// Query

				Sting sting = new Sting();

				sting.setStingid(rs.getInt("stingid"));
				sting.setUsername(rs.getString("username"));
				sting.setAuthor(rs.getString("name"));
				sting.setSubject(rs.getString("subject"));
				sting.setContent(rs.getString("content"));
				oldestTimestamp = rs.getTimestamp("last_modified").getTime();
				sting.setLastModified(oldestTimestamp);

				if (first) {
					first = false;
					stings.setNewestTimestamp(sting.getLastModified());
				}

				stings.addSting(sting);
			}
			stings.setOldestTimestamp(oldestTimestamp);

		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {

			}

		}
		return stings;
	}

	// /////////////////////////////////////////////////////////////////////
	// Obtener stings de un usuario sin QueryParams, paginable
	
	private String GET_STINGS_FROM_USER_QUERY = "SELECT stings.*, users.name FROM stings, users WHERE stings.username=users.username AND stings.creation_timestamp < ifnull(?, now()) and stings.username=? order by creation_timestamp desc limit ?";
	
	@GET
	@Path("/{username}")
	@Produces(MediaType.BEETER_API_STING_COLLECTION)
	public StingCollection getStingsFromUser(
			@PathParam("username") String username,
			@QueryParam("length") int length,
			@QueryParam("before") long before, 
			@QueryParam("after") long after) {
		
		StingCollection stings = new StingCollection();
		
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",	Response.Status.SERVICE_UNAVAILABLE);
		}
		
		PreparedStatement stmt = null;
		try {
			
			stmt = conn.prepareStatement(GET_STINGS_FROM_USER_QUERY);
			
			boolean updateFromLast = after > 0;
			if (updateFromLast) {
				stmt.setTimestamp(1, new Timestamp(after));
			} else {
				if (before > 0)
					stmt.setTimestamp(1, new Timestamp(before));
				else
					stmt.setTimestamp(1, null);
				stmt.setString(2, username);
				length = (length <= 0) ? 5 : length;
				stmt.setInt(3, length);
			}
			ResultSet rs = stmt.executeQuery();
			boolean first = true;
			
			long oldestTimestamp = 0;
			
			while (rs.next()) {
				Sting sting = new Sting();
				sting.setStingid(rs.getInt("stingid"));
				sting.setUsername(rs.getString("username"));
				sting.setAuthor(rs.getString("name"));
				sting.setSubject(rs.getString("subject"));
				oldestTimestamp = rs.getTimestamp("last_modified").getTime();
				sting.setLastModified(oldestTimestamp);
				
				if (first) {
					first = false;
					stings.setNewestTimestamp(sting.getLastModified());
				}
				stings.addSting(sting);
			}
			stings.setOldestTimestamp(oldestTimestamp);
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
		return stings;
	}

	// ///////////////////////////////////////////////////////
	// Método create sting, primero validamos con validateSting

	private void validateSting(Sting sting) {
		if (sting.getSubject() == null)
			throw new BadRequestException("Subject can't be null.");
		if (sting.getContent() == null)
			throw new BadRequestException("Content can't be null.");
		if (sting.getSubject().length() > 100)
			throw new BadRequestException(
					"Subject can't be greater than 100 characters.");
		if (sting.getContent().length() > 500)
			throw new BadRequestException(
					"Content can't be greater than 500 characters.");
	}

	// //
	private String INSERT_STING_QUERY = "insert into stings (username, subject, content) values (?, ?, ?)";

	@POST
	@Consumes(MediaType.BEETER_API_STING)
	@Produces(MediaType.BEETER_API_STING)
	public Sting createSting(Sting sting) {

		validateSting(sting);// /Ejecutamos la validación

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(INSERT_STING_QUERY,
					Statement.RETURN_GENERATED_KEYS);

			// stmt.setString(1, sting.getUsername());
			stmt.setString(1, security.getUserPrincipal().getName());
			stmt.setString(2, sting.getSubject());
			stmt.setString(3, sting.getContent());
			stmt.executeUpdate();
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				int stingid = rs.getInt(1);

				sting = getStingFromDatabase(Integer.toString(stingid));
			} else {
				// Something has failed...
			}
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
				throw new ServerErrorException(
						"Could not connect to the database",
						Response.Status.SERVICE_UNAVAILABLE);
			}
		}

		return sting;
	}

	// ////////////////////////////////////////////
	// Método delete Sting
	private String DELETE_STING_QUERY = "delete from stings where stingid=?";

	@DELETE
	@Path("/{stingid}")
	public void deleteSting(@PathParam("stingid") String stingid) {
		validateUser(stingid);
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(DELETE_STING_QUERY);
			stmt.setInt(1, Integer.valueOf(stingid));

			int rows = stmt.executeUpdate();
			if (rows == 0)// Delete inexistent sting
				throw new NotFoundException("There's no sting with stingid="
						+ stingid);

		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
				throw new ServerErrorException(
						"Could not connect to the database",
						Response.Status.SERVICE_UNAVAILABLE);
			}
		}
	}

	// ////////////////////////////////////
	// Método update sting
	private String UPDATE_STING_QUERY = "update stings set subject=ifnull(?, subject), content=ifnull(?, content) where stingid=?";

	// ifnull -> si no pongo subject dejo el que habia
	@PUT
	@Path("/{stingid}")
	@Consumes(MediaType.BEETER_API_STING)
	@Produces(MediaType.BEETER_API_STING)
	private void validateUpdateSting(Sting sting) {
		if (sting.getSubject() != null && sting.getSubject().length() > 100)
			throw new BadRequestException(
					"Subject can't be greater than 100 characters.");
		if (sting.getContent() != null && sting.getContent().length() > 500)
			throw new BadRequestException(
					"Content can't be greater than 500 characters.");
	}

	public Sting updateSting(@PathParam("stingid") String stingid, Sting sting) {
		validateUpdateSting(sting);
		validateUser(stingid);
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(UPDATE_STING_QUERY);
			stmt.setString(1, sting.getSubject());
			stmt.setString(2, sting.getContent());
			stmt.setInt(3, Integer.valueOf(stingid));

			int rows = stmt.executeUpdate();
			if (rows == 1) // Significa que ha encontrado un sting igual en la
							// base de datos, la búsqueda ha salido bien
				sting = getStingFromDatabase(stingid);
			else {
				;// Updating inexistent sting
			}

		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
				throw new ServerErrorException(
						"Could not connect to the database",
						Response.Status.SERVICE_UNAVAILABLE);
			}
		}

		return sting;
	}

	// //////////////////////////////////////////////
	// Get Sting from Database
	private Sting getStingFromDatabase(String stingid) {
		Sting sting = new Sting();

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(GET_STING_BY_ID_QUERY);
			stmt.setInt(1, Integer.valueOf(stingid));
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				sting.setStingid(rs.getInt("stingid"));
				sting.setUsername(rs.getString("username"));
				sting.setAuthor(rs.getString("name"));
				sting.setSubject(rs.getString("subject"));
				sting.setContent(rs.getString("content"));
				sting.setLastModified(rs.getTimestamp("last_modified")
						.getTime());
				sting.setCreationTimestamp(rs
						.getTimestamp("creation_timestamp").getTime());
			} else {
				throw new NotFoundException("There's no sting with stingid="
						+ stingid);
			}
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}

		return sting;
	}

	// Validar usuario
	private void validateUser(String stingid) {
		Sting sting = getStingFromDatabase(stingid);
		String username = sting.getUsername();
		if (!security.getUserPrincipal().getName().equals(username))
			throw new ForbiddenException(
					"You are not allowed to modify this sting.");
	}
	// //////////////////
}
// ///////////
