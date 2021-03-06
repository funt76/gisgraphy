package com.gisgraphy.importer;

import java.util.ArrayList;
import java.util.List;

import net.sf.jstester.util.Assert;

import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Required;

import com.gisgraphy.addressparser.format.BasicAddressFormater;
import com.gisgraphy.domain.geoloc.entity.Adm;
import com.gisgraphy.domain.repository.AdmDao;
import com.gisgraphy.domain.repository.IAdmDao;
import com.gisgraphy.domain.repository.IIdGenerator;
import com.gisgraphy.domain.valueobject.GISSource;
import com.gisgraphy.fulltext.AbstractIntegrationHttpSolrTestCase;

public class OpenStreetMapAdmSimpleImporterTest extends AbstractIntegrationHttpSolrTestCase {

	private OpenStreetMapAdmSimpleImporter openStreetMapAdmSimpleImporter;
	
	private IAdmDao admDao;
	
LabelGenerator generator = LabelGenerator.getInstance();
	
	BasicAddressFormater formater =  BasicAddressFormater.getInstance();



	@Test
	public void testSetup(){
		OpenStreetMapAdmSimpleImporter importer = new OpenStreetMapAdmSimpleImporter();
		IIdGenerator idGenerator = EasyMock.createMock(IIdGenerator.class);
		idGenerator.sync();
		EasyMock.replay(idGenerator);
		importer.setIdGenerator(idGenerator);

		importer.setup();
		EasyMock.verify(idGenerator);
	}


	@Test
	public void testCalculateAdmLevel() {
		OpenStreetMapAdmSimpleImporter importer = new OpenStreetMapAdmSimpleImporter();
		Assert.assertEquals(1,importer.calculateAdmLevel("US",4));
		Assert.assertEquals(1,importer.calculateAdmLevel("US",4));
		Assert.assertEquals(2,importer.calculateAdmLevel("US",5));
		Assert.assertEquals(2,importer.calculateAdmLevel("US",5));
		Assert.assertEquals(3,importer.calculateAdmLevel("US",6));
		Assert.assertEquals(4,importer.calculateAdmLevel("US",7));
		Assert.assertEquals(5,importer.calculateAdmLevel("US",8));
		Assert.assertEquals(5,importer.calculateAdmLevel("US",9));


		importer = new OpenStreetMapAdmSimpleImporter();
		Assert.assertEquals(1,importer.calculateAdmLevel("US",4));
		Assert.assertEquals(1,importer.calculateAdmLevel("US",4));
		Assert.assertEquals(2,importer.calculateAdmLevel("US",6));

		importer = new OpenStreetMapAdmSimpleImporter();
		Assert.assertEquals(1,importer.calculateAdmLevel("US",4));
		Assert.assertEquals(1,importer.calculateAdmLevel("US",3));//simulate an error
		Assert.assertEquals(2,importer.calculateAdmLevel("US",6));

		importer = new OpenStreetMapAdmSimpleImporter();
		Assert.assertEquals(1,importer.calculateAdmLevel("US",4));
		Assert.assertEquals(1,importer.calculateAdmLevel("US",4));
		Assert.assertEquals(2,importer.calculateAdmLevel("US",6));

		//different country, same level
		importer = new OpenStreetMapAdmSimpleImporter();
		Assert.assertEquals(1,importer.calculateAdmLevel("US",4));
		Assert.assertEquals(1,importer.calculateAdmLevel("US",4));
		Assert.assertEquals(2,importer.calculateAdmLevel("US",6));
		Assert.assertEquals(1,importer.calculateAdmLevel("FR",6));

		//different country,  level <
		importer = new OpenStreetMapAdmSimpleImporter();
		Assert.assertEquals(1,importer.calculateAdmLevel("US",4));
		Assert.assertEquals(1,importer.calculateAdmLevel("US",4));
		Assert.assertEquals(2,importer.calculateAdmLevel("US",6));
		Assert.assertEquals(1,importer.calculateAdmLevel("FR",5));

		//different country,  level >
		importer = new OpenStreetMapAdmSimpleImporter();
		Assert.assertEquals(1,importer.calculateAdmLevel("US",4));
		Assert.assertEquals(1,importer.calculateAdmLevel("US",4));
		Assert.assertEquals(2,importer.calculateAdmLevel("US",6));
		Assert.assertEquals(1,importer.calculateAdmLevel("FR",7));
	}

	@Test
	public void testSetParent(){
		List<AdmDTO> adms = new ArrayList<AdmDTO>();
		AdmDTO admDTO1 = new AdmDTO("Paris", 4, 71525L);
		AdmDTO admDTO2 = new AdmDTO("Île-de-France", 6, 8649L);
		AdmDTO admDTO3 = new AdmDTO("Paris", 7, 1641193L);
		adms.add(admDTO1);
		adms.add(admDTO2);
		adms.add(admDTO3);
		Adm adm = new Adm(5);
		Adm admParent = new Adm(4);
		OpenStreetMapAdmSimpleImporter importer = new OpenStreetMapAdmSimpleImporter();
		AdmDao admDao = EasyMock.createMock(AdmDao.class);
		EasyMock.expect(admDao.getByOpenStreetMapId(1641193L)).andReturn(admParent);
		EasyMock.replay(admDao);
		importer.setAdmDao(admDao);

		importer.setParent(adm, adms);
		Assert.assertEquals(admParent, adm.getParent());
		EasyMock.verify(admDao);

	}
	
	@Test
	public void testSetParent_WrongLevel(){
		List<AdmDTO> adms = new ArrayList<AdmDTO>();
		AdmDTO admDTO1 = new AdmDTO("Paris", 4, 71525L);
		adms.add(admDTO1);
		Adm adm = new Adm(5);
		Adm admParent = new Adm(5);//set osm level != the dto one
		OpenStreetMapAdmSimpleImporter importer = new OpenStreetMapAdmSimpleImporter();
		AdmDao admDao = EasyMock.createMock(AdmDao.class);
		EasyMock.expect(admDao.getByOpenStreetMapId(71525L)).andReturn(admParent);
		EasyMock.replay(admDao);
		importer.setAdmDao(admDao);

		importer.setParent(adm, adms);
		Assert.assertEquals(null, adm.getParent());

	}
	
	@Test
	public void testSetParent_noParentFound(){
		List<AdmDTO> adms = new ArrayList<AdmDTO>();
		Adm adm = new Adm(4);
		OpenStreetMapAdmSimpleImporter importer = new OpenStreetMapAdmSimpleImporter();
		importer.setParent(adm, adms);
		Assert.assertEquals(null, adm.getParent());

	}

	@Test
	public void testProcess(){
		openStreetMapAdmSimpleImporter.process();
		long nbAdm = admDao.count();
		Assert.assertEquals("adm level 8 should not be imported",3, nbAdm);
		
		Adm adm1 = admDao.getByOpenStreetMapId(90348l);
		//Assert.assertEquals(2, adm1.getChildren().size());
		Assert.assertNull(adm1.getParent());
		Assert.assertEquals("Wallonie", adm1.getName());
		Assert.assertEquals("BE", adm1.getCountryCode());
		Assert.assertEquals("BE", adm1.getCountryCode());
		Assert.assertEquals(90348l, adm1.getOpenstreetmapId().longValue());
		Assert.assertNotNull(adm1.getShape());
		Assert.assertNotNull(adm1.getLocation());
		
		Assert.assertEquals(21, adm1.getAlternateNames().size());
		
		Assert.assertEquals(generator.generateLabel(adm1), adm1.getLabel());
		Assert.assertNotNull(adm1.getLabel());
		
		Assert.assertNotNull(adm1.getFullyQualifiedName());
		Assert.assertEquals(generator.generateLabel(adm1), adm1.getFullyQualifiedName());
		
		
		Assert.assertNotNull(adm1.getFeatureId());
		Assert.assertEquals(GISSource.OSM, adm1.getSource());
		
		
		
		
		Adm adm2 = admDao.getByOpenStreetMapId(1412581l);
		Assert.assertEquals(2, adm2.getLevel().intValue());
		Assert.assertNotNull(adm2.getParent().getOpenstreetmapId());
		Assert.assertEquals("Wallonie", adm2.getAdm1Name());
		Assert.assertEquals("departement", adm2.getAmenity());


	}

	@Required
	public void setOpenStreetMapAdmSimpleImporter(
			OpenStreetMapAdmSimpleImporter openStreetMapAdmSimpleImporter) {
		this.openStreetMapAdmSimpleImporter = openStreetMapAdmSimpleImporter;
	}


	@Required
	public void setAdmDao(IAdmDao admDao) {
		this.admDao = admDao;
	}


}
