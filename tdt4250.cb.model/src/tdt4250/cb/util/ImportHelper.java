package tdt4250.cb.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import tdt4250.cb.Bike;
import tdt4250.cb.CbFactory;
import tdt4250.cb.City;
import tdt4250.cb.Station;
import tdt4250.cb.Trip;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ImportHelper {

	/**
	 * Fetches station data from Trondheim Bysykkel and adds them to the city object
	 * @param city - The City that owns the stations
	 */
	public static void addStations(City city){
		ObjectMapper mapper = new ObjectMapper();
		try
		{
			HttpClient client = HttpClient.newHttpClient();
	        HttpRequest request = HttpRequest.newBuilder()
	                .uri(URI.create("https://gbfs.urbansharing.com/trondheimbysykkel.no/station_information.json"))
	                .build();

	        HttpResponse<String> response = client.send(request,
	        		HttpResponse.BodyHandlers.ofString());
        	
			JsonNode node = mapper.readTree(response.body());
			jsonToStations(node, city);
		}
		catch (Exception e) {
	        System.out.println(e.toString());
	    }
	}
	
	/**
	 * Fetches trip data from Trondheim Bysykkel and adds them to the city object
	 * @param city - The City that owns the trips
	 */
	public static void addTrips(City city) {
		ObjectMapper mapper = new ObjectMapper();
		try
		{
			HttpClient client = HttpClient.newHttpClient();
	        HttpRequest request = HttpRequest.newBuilder()
	                .uri(URI.create("https://data.urbansharing.com/trondheimbysykkel.no/trips/v1/2020/10.json"))
	                .build();

	        HttpResponse<String> response = client.send(request,
	        		HttpResponse.BodyHandlers.ofString());
        	
			JsonNode node = mapper.readTree(response.body());
			jsonToTrips(node, city);
			
		}
		catch (Exception e) {
	        System.out.println(e.toString());
	    }
	}
	
	/**
	 * Creates bike objects and adds them to the city object
	 * @param city - The City that owns the bikes
	 */
	public static void addBikes(City city) {
		CbFactory factory = CbFactory.eINSTANCE;
		ArrayList<String> names = getNames();
		for (int i = 0; i < 100; i++) {
			
			Bike bike = factory.createBike();
			bike.setName(names.get(i%names.size()));
			try {
				parkBike(city, bike);
			} catch (Exception e) {
				e.printStackTrace();
			}
			city.getBikes().add(bike);
		}
		
	}
		
	// PRIVATE METHODS
	
	/**
	 * Returns names.txt as a list of names
	 * @return ArrayList of names (strings)
	 */
	private static ArrayList<String> getNames() {
		ArrayList<String> names = new ArrayList<>();
	    try {
	        File myObj = new File("./src/resources/names.txt");
	        Scanner myReader = new Scanner(myObj);
	        while (myReader.hasNextLine()) {
	          String data = myReader.nextLine();
	          names.add(data);
	        }
	        myReader.close();
	      } catch (FileNotFoundException e) {
	    	  System.out.println("An error occurred.");
	    	  e.printStackTrace();
	      }
		return names;
	}
	

	
	/**
	 * Converts a date string to Date object
	 * @param date - A string on the format yyy-MM-dd kk:mm:ss
	 * @return The converted Date
	 * @throws ParseException
	 */
	private static Date stringToDate(String date) throws ParseException {
		   DateFormat df = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss", Locale.ENGLISH);
		   Date result =  df.parse(date);
		   return result;
	}
	
	/**
	 * Searches through the stations in the city and returns the one matching the id
	 * @param id - The ID of the station that you want to fetch
	 * @param city - City that contains the station
	 * @return The station with the id
	 * @throws Exception if it can't find a station with the id
	 */
	private static Station getStationById(int id, City city) throws Exception {
		for (Station station : city.getStations()) {
			if(station.getId() == id) {
				return station;
			}
		}
		throw new Exception("Could not find station by ID: " + id);
	}
	
	
	/**
	 * Parks a bike, meaning connecting a bike to a random station and add the bike to Station.AvailableBikes
	 * @param city - The city that contains the bikes and stations
	 * @param bike - The bike to be parked
	 * @throws Exception if it can't find any available stations after 100 tries. 
	 */
	private static void parkBike(City city, Bike bike) throws Exception {
		Random rand = new Random();
		Station station = city.getStations().get(rand.nextInt(city.getStations().size()));
		
		int loopCounter = 0;
		// If station has no available docks, pick a new one
		while(station.getAvailableDocksNum() == 0) {
			station = city.getStations().get(rand.nextInt(city.getStations().size()));
			if(loopCounter == 100) {
				throw new Exception("Could not find any stations to park bike");
			}
			loopCounter ++;
			System.out.println(loopCounter);
		}
		
		bike.setCurrentStation(station);
		station.setAvailableDocksNum(station.getAvailableDocksNum()-1);
		station.getAvailableBikes().add(bike);
	}
	
	
	/**
	 * Translate JSON to Trip objects and add them to the city
	 * @param json - JSON object that contains information about trips
	 * @param city - The city that the trips will be added to
	 * @throws Exception
	 */
	private static void  jsonToTrips(JsonNode json, City city) throws Exception {
		
		//for (int i = 0; i < json.size(); i++) {
		//TODO: Figure out how many trips to add. json.size contains about 2500 trips
		
		for (int i = 0; i < 50; i++) {
			JsonNode tripJson = json.get(i);
			
			CbFactory factory = CbFactory.eINSTANCE;
			Trip trip = factory.createTrip();
			int startStationId = tripJson.get("start_station_id").asInt();
			int endStationId = tripJson.get("end_station_id").asInt();
			int duration = tripJson.get("duration").asInt();
			
			trip.setId(i+1);
			trip.setStartStation(getStationById(startStationId, city));
			trip.setEndStation(getStationById(endStationId, city));
			trip.setDuration(duration);
			trip.setStartTime(stringToDate(tripJson.get("started_at").asText()));
			trip.setEndTime(stringToDate(tripJson.get("ended_at").asText()));
			city.getTrips().add(trip);
		}
	}
	
	
	/**
	 * Translate JSON to Station objects and add them to the city
	 * @param json - JSON object that contains information about trips
	 * @param city - The city that the trips will be added to
	 * @throws Exception
	 */
	private static void jsonToStations(JsonNode json, City city){
		JsonNode stationsJson = json.get("data").get("stations");
		for (int i = 0; i < stationsJson.size(); i++) {
			JsonNode stationJson = stationsJson.get(i);
			
			CbFactory factory = CbFactory.eINSTANCE;

			Station station = factory.createStation();
			
			station.setName(stationJson.get("name").asText());
			station.setAddress(stationJson.get("address").asText());
			station.setCapacityNum(stationJson.get("capacity").asInt());
			station.setId(stationJson.get("station_id").asInt());
			station.setYCoordinate(stationJson.get("lat").floatValue());
			station.setXCoordinate(stationJson.get("lon").floatValue());
			station.setAvailableDocksNum(stationJson.get("capacity").asInt());

			city.getStations().add(station);
		}
	}
	
}
