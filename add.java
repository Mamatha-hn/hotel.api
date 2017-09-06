package com.rakuten.payvault.api.cardtoken.add.controller;

import static com.rakuten.payvault.common.http.HttpHeader.HSTS_VALUE_MAX_AGE;
import static com.rakuten.payvault.common.http.HttpHeader.STRICT_TRANSPORT_SECURITY;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.rakuten.payvault.api.ApiMessage;
import com.rakuten.payvault.api.BaseController;
import com.rakuten.payvault.api.BaseResponse;
import com.rakuten.payvault.api.cardtoken.add.model.AddRequest;
import com.rakuten.payvault.api.cardtoken.add.model.AddResponse;
import com.rakuten.payvault.api.cardtoken.add.service.AddService;
import com.rakuten.payvault.api.model.FullCardDetails;
import com.rakuten.payvault.common.code.ErrorCode;
import com.rakuten.payvault.common.exception.PayVaultBizException;
import com.rakuten.payvault.common.util.CardUtil;
import com.rakuten.payvault.common.util.ObjectUtil;
import com.rakuten.payvault.common.validation.AnnotationValidator;

/**
 * Controller of Add API
 *
 * @author ts-soenaing.kyaw
 */
@Controller
public class AddController extends BaseController {
    // constants
    /* ======================================================================== */
    /** Logger object */
    private static final Log LOGGER = LogFactory.getLog(AddController.class);

    // fields
    /* ======================================================================== */
    /** Add service */
    @Autowired
    private AddService addService;

    // public methods
    /* ======================================================================== */
    /**
     * Controller of Add API version 1<br>
     * Request format : JSON
     *
     * @param httpRequest
     * @param httpResponse
     * @param addRequestString
     * @return JSON string
     */
    @RequestMapping(value = "/pv/Card/V1/Add", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String add(HttpServletRequest httpRequest, HttpServletResponse httpResponse, @RequestBody String addRequestString) {
        String serviceId = null;
        try {
            // decode JSON
            AddRequest addRequest = null;
            try {
                addRequest = jsonConverter.decode(addRequestString, AddRequest.class);
                serviceId = addRequest.getServiceId();
                // mask card number for log
                String maskedRequestString = jsonConverter.encode(maskRequest(addRequest));
                LOGGER.info(formatLog("101-1-1", serviceId, "[request] " + maskedRequestString));
            } catch (Throwable e) {
                throw new PayVaultBizException("101-1-2", ErrorCode.INVALID_REQUEST_PARAMETER, ApiMessage.get("T_006"))
                        .addLogMessage(e.getMessage());
            }

            // input validation
            String validationErrorMessage = AnnotationValidator.getViolationMessage(addRequest);
            if (StringUtils.isNotEmpty(validationErrorMessage)) {
                throw new PayVaultBizException("101-1-3", ErrorCode.INVALID_REQUEST_PARAMETER, ApiMessage.get("T_002")
                        + validationErrorMessage);
            }

            // execute Add service
            AddResponse addResponse = addService.execute(addRequest);

            // add security HTTP header
            httpResponse.setHeader(STRICT_TRANSPORT_SECURITY, HSTS_VALUE_MAX_AGE);

            // create response
            return createResponse(addResponse, serviceId);

        } catch (PayVaultBizException ex) {
            LOGGER.info(formatLog(ex.getPlaceCode(), serviceId, ex.getLogMessage()));
            return createResponse(new BaseResponse(ex.getErrorCode().getCode(), ex.getMessage()), serviceId);
        } catch (Throwable ex) {
            LOGGER.warn(formatLog("101-1-4", serviceId, "", ex));
            return createResponse(new BaseResponse(ErrorCode.SYSTEM_ERROR.getCode(), ApiMessage.get("T_005")), serviceId);
        }
    }

    // private methods
    /* ======================================================================== */
    /**
     * Mask card number for log
     *
     * @param addRequest
     * @return a copy of request with masked card number
     */
    private AddRequest maskRequest(AddRequest addRequest) {
        AddRequest maskedRequest = ObjectUtil.deepCopy(addRequest);
        FullCardDetails fullCardDetails = maskedRequest.getFullCardDetails();
        if (fullCardDetails != null) {
            fullCardDetails.setCardNumber(CardUtil.maskCardNumber(fullCardDetails.getCardNumber()));
        }
        return maskedRequest;
    }
}
