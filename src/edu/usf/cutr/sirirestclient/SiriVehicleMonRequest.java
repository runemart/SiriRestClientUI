/*
 * Copyright 2012 University of South Florida
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package edu.usf.cutr.sirirestclient;

/**
 * Spring imports
 */
//import org.springframework.android.showcase.rest.State;
//import org.springframework.android.showcase.rest.StatesListAdapter;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * SIRI imports
 */
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.VehicleMonitoringDelivery;

/**
 * Java imports
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Android imports
 */
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.res.Resources.NotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

/**
 * Hacks to get JAXB classes to compile on Android
 * see: http://www.docx4java.org/blog/2012/05/jaxb-can-be-made-to-run-on-android/
 */
//import ae.com.sun.xml.bind.v2.model.annotation.RuntimeInlineAnnotationReader;
//import ae.com.sun.xml.bind.v2.model.annotation.XmlSchemaMine;
//import ae.javax.xml.bind.annotation.XmlAccessType;

/**
 * The UI for the input fields for the SIRI Vehicle Monitoring Request
 * 
 * @author Sean Barbeau
 *
 */
public class SiriVehicleMonRequest extends Fragment {
 
  private ProgressDialog progressDialog;

  private boolean destroyed = false;
  
  /**
   * EditText fields to hold values typed in by user
   */
  EditText key;
  EditText operatorRef;
  EditText vehicleRef;
  EditText lineRef;
  EditText directionRef;
  EditText vehicleMonitoringDetailLevel;
  EditText maximumNumberOfCallsOnwards;
      
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
//      RuntimeInlineAnnotationReader.cachePackageAnnotation(
//          MonitoredVehicleJourneyStructure.class.getPackage(), new XmlSchemaMine("uk.org.siri.siri.MonitoredVehicleJourneyStructure"));
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
          Bundle savedInstanceState) {
      View v = inflater.inflate(R.layout.siri_vehicle_mon_request, container, false);
            
      //Try to get the developer key from a resource file, if it exists
      String strKey = getKeyFromResource(); 
            
      key = (EditText) v.findViewById(R.id.key); 
      key.setText(strKey);
      operatorRef = (EditText) v.findViewById(R.id.operatorRef);
      vehicleRef = (EditText) v.findViewById(R.id.vehicleRef);
      lineRef = (EditText) v.findViewById(R.id.lineRef);
      directionRef = (EditText) v.findViewById(R.id.directionRef);
      vehicleMonitoringDetailLevel = (EditText) v.findViewById(R.id.vehicleMonDetailLevel);
      maximumNumberOfCallsOnwards= (EditText) v.findViewById(R.id.maxNumOfCallsOnwards);
      
      final Button button = (Button) v.findViewById(R.id.submit);
      
      button.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
                           
          //Start Async task to make REST request
          new DownloadVehicleInfoTask().execute();
          
          //TODO Get response back and show in another tab, then switch to that tab
        }
      });
      
      return v;
  }
  
  @Override
  public void onDestroy() {
    // TODO Auto-generated method stub
    super.onDestroy();
    destroyed = true;
  }
  
  // ***************************************
  // Private methods
  // ***************************************
  private void refreshStates(List<ServiceDelivery> states) {
      if (states == null) {
          return;
      }

      //StatesListAdapter adapter = new StatesListAdapter(this, states);
      //setListAdapter(adapter);
  }
  
  /**
   * Try to grab the developer key from an unversioned resource file, if it exists
   * @return the developer key from an unversioned resource file, or empty string if it doesn't exist
   */
  private String getKeyFromResource(){
    String strKey = new String("");
    
    try {
      InputStream in = getResources().openRawResource(R.raw.devkey);
      BufferedReader r = new BufferedReader(new InputStreamReader(in));
      StringBuilder total = new StringBuilder();
      
      while ((strKey = r.readLine()) != null) {
          total.append(strKey);
      }
      
      strKey = total.toString();
      
      strKey.trim();  //Remove any whitespace
                  
    } catch (NotFoundException e) {
      // TODO Auto-generated catch block
      Log.w(SiriRestClientActivity.TAG, "Warning - didn't find the developer key file:" + e);        
    } catch (IOException e) {
      // TODO Auto-generated catch block
      Log.w(SiriRestClientActivity.TAG, "Error reading the developer key file:" + e);
    }
    
    return strKey;
  }
  
  //***************************************
  // Private classes
  // ***************************************
  private class DownloadVehicleInfoTask extends AsyncTask<Void, Void, List<ServiceDelivery>> {
    //TODO - need to add JAXB libraries to Maven dependencies??
      @Override
      protected void onPreExecute() {
          // before the network request begins, show a progress indicator
          showLoadingProgressDialog();
      }

      @Override
      protected List<ServiceDelivery> doInBackground(Void... params) {
          try {
              // The URL for making the GET request
              String url = "http://bustime.mta.info/api/siri" + "/vehicle-monitoring.json?OperatorRef=MTA NYCT";
//              final String url = "http://bustime.mta.info/api/siri" + "/vehicle-monitoring.json?" +
//              		"key={key}&OperatorRef={operatorRef}&VehicleRef={vehicleRef}&LineRef={lineRef}&DirectionRef={directionRef}" +
//              		"&VehicleMonitoringDetailLevel={vehicleMonitoringDetailLevel}&MaximumNumberOfCallsOnwards={maximumNumberOfCallsOnwards}";
              
              //Sample vehicle request: http://bustime.mta.info/api/siri/vehicle-monitoring.json?OperatorRef=MTA%20NYCT&DirectionRef=0&LineRef=MTA%20NYCT_S40
              //Sample stop monitoring request: http://bustime.mta.info/api/siri/stop-monitoring.json?OperatorRef=MTA%20NYCT&MonitoringRef=308214
              url.replace(" ", "%20");  //Handle spaces
            
              // Set the Accept header for "application/json"
              HttpHeaders requestHeaders = new HttpHeaders();
              List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
              acceptableMediaTypes.add(MediaType.APPLICATION_JSON);
              //acceptableMediaTypes.add(MediaType.TEXT_PLAIN);
              requestHeaders.setAccept(acceptableMediaTypes);

              // Populate the headers in an HttpEntity object to use for the request
              HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
              
              // Create a new RestTemplate instance
              RestTemplate restTemplate = new RestTemplate();
              restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
              //restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
                         
              // Perform the HTTP GET request w/ specified parameters
              ResponseEntity<ServiceDelivery[]> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, ServiceDelivery[].class);
//              ResponseEntity<Siri[]> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Siri[].class, 
//                  key.getText().toString(), operatorRef.getText().toString(), vehicleRef.getText().toString(), 
//                  lineRef.getText().toString(), directionRef.getText().toString(), 
//                  vehicleMonitoringDetailLevel.getText().toString(), maximumNumberOfCallsOnwards.getText().toString());
                            
              //convert the array to a list
              List<ServiceDelivery> list = Arrays.asList(responseEntity.getBody());
              
//              for(Siri l : list){
//                List<VehicleMonitoringDelivery> listVMD = l.getServiceDelivery().getVehicleMonitoringDelivery();
//                for(VehicleMonitoringDelivery v : listVMD){
//                  Log.d(SiriRestClientActivity.TAG, "ResponseTime = " + v.getResponseTimestamp());
//                  Log.d(SiriRestClientActivity.TAG, "ValidUntil = " + v.getValidUntil());
//                                    
//                }
//              }
              
              // return list
              return list;
          } catch (Exception e) {
              Log.e(SiriRestClientActivity.TAG, e.getMessage(), e);
          }

          return null;
      }

      @Override
      protected void onPostExecute(List<ServiceDelivery> result) {
          // hide the progress indicator when the network request is complete
          dismissProgressDialog();
          
          

          // return the list of vehicle info
          refreshStates(result);          
      }

   // ***************************************
      // Public methods
      // ***************************************
      public void showLoadingProgressDialog() {
          this.showProgressDialog("Requesting. Please wait...");
      }

      public void showProgressDialog(CharSequence message) {
          if (progressDialog == null) {
              progressDialog = new ProgressDialog(getActivity());
              progressDialog.setIndeterminate(true);
          }

          progressDialog.setMessage(message);
          progressDialog.show();
      }

      public void dismissProgressDialog() {
          if (progressDialog != null && !destroyed) {
              progressDialog.dismiss();
          }
      }
  }
}