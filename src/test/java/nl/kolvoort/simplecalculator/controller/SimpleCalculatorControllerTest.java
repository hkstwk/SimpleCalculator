package nl.kolvoort.simplecalculator.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.kolvoort.simplecalculator.dto.CalculationInput;
import nl.kolvoort.simplecalculator.dto.CalculationOutput;
import nl.kolvoort.simplecalculator.service.SimpleCalculatorServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest(SimpleCalculatorController.class)
public class SimpleCalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SimpleCalculatorServiceImpl simpleCalculatorServiceMockBean;

    @Test
    void singleCalculationShouldReturnOk() throws Exception {
        when(simpleCalculatorServiceMockBean.doCalculation(5, 2, "+"))
                .thenReturn(7.0);
        CalculationInput calculationInput = new CalculationInput();
        calculationInput.setOperator("+");
        calculationInput.setLeftOperand(5);
        calculationInput.setRightOperand(2);
        String jsonString = objectToString(calculationInput);
        log.info("DTO CalculationInput = {}", jsonString);

        this.mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/single")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("result").value("7.0"));

        verify(simpleCalculatorServiceMockBean, times(1)).doCalculation(anyInt(), anyInt(), anyString());
    }

    @Test
    void multipleCalculationShouldReturnOk() throws Exception {

        // Arrange
        List<CalculationInput> calculations = new ArrayList<>();
        calculations.add(new CalculationInput(55, 21, "+"));
        calculations.add(new CalculationInput(65, 22, "-"));
        calculations.add(new CalculationInput(75, 23, "*"));
        calculations.add(new CalculationInput(85, 24, "/"));
        double expected = 373.25;

        log.info("Test input multiple calculations = {}", objectToString(calculations));

        when(simpleCalculatorServiceMockBean.doCalculation(anyInt(), anyInt(), anyString())).thenReturn(expected);

        // Act
        MvcResult mvcResult = this.mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/multiple")
                        .content(objectToString(calculations))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[*].result", hasItem(Double.toString(expected))))
                .andReturn();

        // Assert
        String response = mvcResult.getResponse().getContentAsString();
        log.info("Test output multiple calculations = {}", response);

        List<CalculationOutput> contentAsList= new ObjectMapper().readValue(response, new TypeReference<>(){});
        assertEquals(4, contentAsList.size());

        verify(simpleCalculatorServiceMockBean, times(calculations.size())).doCalculation(anyInt(), anyInt(), anyString());
    }

    @Test
    void singleCalculationUnknownOperatorShouldReturnUError() throws Exception {
        // Arrange
        CalculationInput calculationInput = new CalculationInput();
        calculationInput.setOperator("%");
        calculationInput.setLeftOperand(5);
        calculationInput.setRightOperand(2);
        IllegalArgumentException exception = new IllegalArgumentException("Operator not supported");

        when(simpleCalculatorServiceMockBean.doCalculation(5, 2, "%"))
                .thenThrow(new IllegalArgumentException("Operator not supported"));

        String jsonString = objectToString(calculationInput);
        log.info("DTO CalculationInput = {}", jsonString);

        // Act & Assert
        this.mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/single")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("result")
                        .value(exception));

        verify(simpleCalculatorServiceMockBean, times(1)).doCalculation(anyInt(), anyInt(), anyString());
    }

    private String objectToString(Object object) throws Exception {
        return new ObjectMapper().writeValueAsString(object);
    }

}
