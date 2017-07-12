package de.tu_berlin.open_data.weather.batch;

import de.tu_berlin.ise.open_data.service.ApplicationService;
import de.tu_berlin.open_data.weather.http.HttpService;
import de.tu_berlin.open_data.weather.model.BMESensor;
import de.tu_berlin.open_data.weather.model.DHTSensor;
import de.tu_berlin.open_data.weather.model.SDSAndPPDSensor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;


/**
 * Created by ahmadjawid on 5/21/17.
 */

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
    @Value("${resource.url}")
    private String resourceUrl;


    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    public ApplicationService applicationService;

    @Autowired
    @Qualifier("dataSource")
    public DataSource dataSource;

    @Autowired
    private HttpService httpFileDownloaderService;

    @Bean
    public MultiResourceItemReader readerBME() throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        MultiResourceItemReader multiResourceItemReader = new MultiResourceItemReader();
        String sensorType = "bme";
        multiResourceItemReader.setResources(httpFileDownloaderService.getUrlResources(resourceUrl, sensorType));

        FlatFileItemReader reader = new FlatFileItemReader<>();

        reader.setLinesToSkip(1);


        reader.setLineMapper(applicationService.createLineMapper(BMESensor.class));

        multiResourceItemReader.setDelegate(reader);
        return multiResourceItemReader;
    }


    @Bean
    public MultiResourceItemReader readerDHT() throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        MultiResourceItemReader multiResourceItemReader = new MultiResourceItemReader();
        String sensorType = "dht";
        multiResourceItemReader.setResources(httpFileDownloaderService.getUrlResources(resourceUrl, sensorType));

        FlatFileItemReader reader = new FlatFileItemReader<>();

        reader.setLinesToSkip(1);

        reader.setLineMapper(applicationService.createLineMapper(DHTSensor.class));

        multiResourceItemReader.setDelegate(reader);
        return multiResourceItemReader;
    }


    @Bean
    public MultiResourceItemReader readerSDS() throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        MultiResourceItemReader multiResourceItemReader = new MultiResourceItemReader();
        String sensorType = "sds";
        multiResourceItemReader.setResources(httpFileDownloaderService.getUrlResources(resourceUrl, sensorType));

        FlatFileItemReader reader = new FlatFileItemReader<>();

        reader.setLinesToSkip(1);

        reader.setLineMapper(applicationService.createLineMapper(SDSAndPPDSensor.class));

        multiResourceItemReader.setDelegate(reader);
        return multiResourceItemReader;
    }


    @Bean
    public MultiResourceItemReader readerPPD() throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        MultiResourceItemReader multiResourceItemReader = new MultiResourceItemReader();
        String sensorType = "ppd";
        multiResourceItemReader.setResources(httpFileDownloaderService.getUrlResources(resourceUrl, sensorType));

        FlatFileItemReader reader = new FlatFileItemReader<>();

        reader.setLinesToSkip(1);


        reader.setLineMapper(applicationService.createLineMapper(SDSAndPPDSensor.class));

        multiResourceItemReader.setDelegate(reader);
        return multiResourceItemReader;
    }


    @Bean
    public BMESensorItemProcessor bmeSensorItemProcessor() {
        return new BMESensorItemProcessor();
    }

    @Bean
    public DHTSensorItemProcessor dhtSensorItemProcessor(){
        return new DHTSensorItemProcessor();
    }

    @Bean
    public SDSAndPPDSensorItemProcessor sdsAndPPDSensorItemProcessor(){
        return new SDSAndPPDSensorItemProcessor();
    }

    @Bean
    public JsonItemWriter writer() {
        return new JsonItemWriter();
    }

    @Bean
    public Job weatherDataJob(JobCompletionNotificationListener listener) throws NoSuchMethodException, IllegalAccessException, InstantiationException, ClassNotFoundException, InvocationTargetException, MalformedURLException {
        return jobBuilderFactory.get("weatherDataJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(step1()).next(step2()).next(step3()).next(step4())
                .end()
                .build();
    }

    @Bean
    public Step step1() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, MalformedURLException, ClassNotFoundException {
        return stepBuilderFactory.get("step1")
                .<BMESensor, BMESensor>chunk(10000)
                .reader(readerBME())
                .processor(bmeSensorItemProcessor())
                .writer(writer())
                .build();
    }


    @Bean
    public Step step2() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, MalformedURLException, ClassNotFoundException {
        return stepBuilderFactory.get("step2")
                .<BMESensor, BMESensor>chunk(10000)
                .reader(readerDHT())
                .processor(dhtSensorItemProcessor())
                .writer(writer())
                .build();
    }

    @Bean
    public Step step3() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, MalformedURLException, ClassNotFoundException {
        return stepBuilderFactory.get("step3")
                .<BMESensor, BMESensor>chunk(10000)
                .reader(readerSDS())
                .processor(sdsAndPPDSensorItemProcessor())
                .writer(writer())
                .build();
    }


    @Bean
    public Step step4() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, MalformedURLException, ClassNotFoundException {
        return stepBuilderFactory.get("step4")
                .<BMESensor, BMESensor>chunk(10000)
                .reader(readerPPD())
                .processor(sdsAndPPDSensorItemProcessor())
                .writer(writer())
                .build();
    }

    //The configuration used to manually fetching a particular job from job registry and executing it
    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcess(JobRegistry jobRegistry) {
        JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor = new JobRegistryBeanPostProcessor();
        jobRegistryBeanPostProcessor.setJobRegistry(jobRegistry);
        return jobRegistryBeanPostProcessor;
    }
}