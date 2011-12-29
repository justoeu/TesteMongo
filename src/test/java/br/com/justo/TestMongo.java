package br.com.justo;

import java.net.UnknownHostException;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

public class TestMongo {

	Mongo mongo = null;
	
	@Before
	public void setUp() throws UnknownHostException, MongoException{
		mongo = new Mongo( "127.0.0.1" );
	}
	
	@Test
	public void save(){
		
		DB db = mongo.getDB("admin");
		
		String pass = "teste";
		
		boolean auth = db.authenticate("valmir", pass.toCharArray() );
		
		System.out.println(auth);
		
		db = mongo.getDB("paymentDB");
		
		//auth = db.authenticate("valmir", pass.toCharArray() );
		//System.out.println(auth);
		
		// for (String dbs : mongo.getDatabaseNames()) {
	    //        System.out.println(dbs);
	    //    }
		
		Set<String> colls = db.getCollectionNames();

		for (String s : colls) {
		    System.out.println(s);
		}

		
		DBCollection coll = db.getCollection("dados");
		
		BasicDBObject doc = new BasicDBObject();

        doc.put("name", "MongoDB");
        doc.put("type", "database");
        doc.put("count", 1);

        BasicDBObject info = new BasicDBObject();

        info.put("x", 203);
        info.put("y", 102);

        doc.put("info", info);

        WriteResult res = coll.insert(doc);
        
        String valor = (String) res.getField("serverUsed");
        
        System.out.println(valor);
        
        DBObject myDoc = coll.findOne();
        System.out.println(myDoc);
        
        System.out.println("Qtd Regs: " + coll.getCount() );

        DBCursor cursor = coll.find();
        
        for (DBObject dbObject : cursor) {
			System.out.println(dbObject.get("info"));
		}
        
        
        BasicDBObject query = new BasicDBObject();

        query.put("count", 1);
        
        cursor = coll.find(query);

        while(cursor.hasNext()) {
            System.out.println(cursor.next());
        }
        
        
	}
	
}