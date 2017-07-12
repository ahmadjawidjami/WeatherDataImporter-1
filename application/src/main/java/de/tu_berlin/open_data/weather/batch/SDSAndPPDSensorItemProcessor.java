package de.tu_berlin.open_data.weather.batch;

import de.tu_berlin.ise.open_data.service.ApplicationService;
import de.tu_berlin.open_data.weather.model.SDSAndPPDSensor;
import de.tu_berlin.open_data.weather.service.JsonSchemaCreator;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Created by ahmadjawid on 6/20/17.
 */
public class SDSAndPPDSensorItemProcessor implements ItemProcessor<SDSAndPPDSensor, String> {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    @Qualifier("SDSAndPPDSensorJsonSchemaCreator")
    private JsonSchemaCreator jsonSchemaCreator;
    @Override
    public String process(SDSAndPPDSensor sdsAndPPDSensorItem) throws Exception {
        sdsAndPPDSensorItem.setTimestamp(applicationService.toISODateFormat(sdsAndPPDSensorItem.getTimestamp()));

        return jsonSchemaCreator.create(sdsAndPPDSensorItem);
    }
}
