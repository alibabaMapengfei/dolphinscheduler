/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.api.controller;

import static org.apache.dolphinscheduler.api.enums.Status.CREATE_K8S_NAMESPACE_ERROR;
import static org.apache.dolphinscheduler.api.enums.Status.DELETE_K8S_NAMESPACE_BY_ID_ERROR;
import static org.apache.dolphinscheduler.api.enums.Status.QUERY_AUTHORIZED_NAMESPACE_ERROR;
import static org.apache.dolphinscheduler.api.enums.Status.QUERY_CAN_USE_K8S_NAMESPACE_ERROR;
import static org.apache.dolphinscheduler.api.enums.Status.QUERY_K8S_NAMESPACE_LIST_PAGING_ERROR;
import static org.apache.dolphinscheduler.api.enums.Status.QUERY_UNAUTHORIZED_NAMESPACE_ERROR;
import static org.apache.dolphinscheduler.api.enums.Status.VERIFY_K8S_NAMESPACE_ERROR;

import org.apache.dolphinscheduler.api.exceptions.ApiException;
import org.apache.dolphinscheduler.api.service.K8sNamespaceService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.dao.entity.K8sNamespace;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.plugin.task.api.utils.ParameterUtils;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * k8s namespace controller
 */
@Tag(name = "K8S_NAMESPACE_TAG")
@RestController
@RequestMapping("/k8s-namespace")
public class K8sNamespaceController extends BaseController {

    @Autowired
    private K8sNamespaceService k8sNamespaceService;

    /**
     * query namespace list paging
     *
     * @param loginUser login user
     * @param searchVal search value
     * @param pageSize  page size
     * @param pageNo    page number
     * @return namespace list which the login user have permission to see
     */
    @Operation(summary = "queryNamespaceListPaging", description = "QUERY_NAMESPACE_LIST_PAGING_NOTES")
    @Parameters({
            @Parameter(name = "searchVal", description = "SEARCH_VAL", schema = @Schema(implementation = String.class)),
            @Parameter(name = "pageSize", description = "PAGE_SIZE", required = true, schema = @Schema(implementation = int.class, example = "10")),
            @Parameter(name = "pageNo", description = "PAGE_NO", required = true, schema = @Schema(implementation = int.class, example = "1"))
    })
    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_K8S_NAMESPACE_LIST_PAGING_ERROR)
    public Result queryNamespaceListPaging(@Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                           @RequestParam(value = "searchVal", required = false) String searchVal,
                                           @RequestParam("pageSize") Integer pageSize,
                                           @RequestParam("pageNo") Integer pageNo) {

        checkPageParams(pageNo, pageSize);
        searchVal = ParameterUtils.handleEscapes(searchVal);
        return k8sNamespaceService.queryListPaging(loginUser, searchVal, pageNo, pageSize);
    }

    /**
     * create namespace,if not exist on k8s,will create,if exist only register in db
     *
     * @param loginUser
     * @param namespace    k8s namespace
     * @param clusterCode  clusterCode
     * @return
     */
    @Operation(summary = "createK8sNamespace", description = "CREATE_NAMESPACE_NOTES")
    @Parameters({
            @Parameter(name = "namespace", description = "NAMESPACE", required = true, schema = @Schema(implementation = String.class)),
            @Parameter(name = "clusterCode", description = "CLUSTER_CODE", required = true, schema = @Schema(implementation = long.class))
    })
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    @ApiException(CREATE_K8S_NAMESPACE_ERROR)
    public Result createNamespace(@Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                  @RequestParam(value = "namespace") String namespace,
                                  @RequestParam(value = "clusterCode") Long clusterCode) {
        Map<String, Object> result =
                k8sNamespaceService.createK8sNamespace(loginUser, namespace, clusterCode);
        return returnDataList(result);
    }

    /**
     * verify namespace and k8s,one k8s namespace is unique
     *
     * @param loginUser   login user
     * @param namespace   namespace
     * @param clusterCode cluster code
     * @return true if the k8s and namespace not exists, otherwise return false
     */
    @Operation(summary = "verifyNamespaceK8s", description = "VERIFY_NAMESPACE_K8S_NOTES")
    @Parameters({
            @Parameter(name = "namespace", description = "NAMESPACE", required = true, schema = @Schema(implementation = String.class)),
            @Parameter(name = "clusterCode", description = "CLUSTER_CODE", required = true, schema = @Schema(implementation = long.class)),
    })
    @PostMapping(value = "/verify")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(VERIFY_K8S_NAMESPACE_ERROR)
    public Result verifyNamespace(@Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                  @RequestParam(value = "namespace") String namespace,
                                  @RequestParam(value = "clusterCode") Long clusterCode) {

        return k8sNamespaceService.verifyNamespaceK8s(namespace, clusterCode);
    }

    /**
     * delete namespace by id
     *
     * @param loginUser login user
     * @param id        namespace id
     * @return delete result code
     */
    @Operation(summary = "delNamespaceById", description = "DELETE_NAMESPACE_BY_ID_NOTES")
    @Parameters({
            @Parameter(name = "id", description = "NAMESPACE_ID", required = true, schema = @Schema(implementation = int.class, example = "100"))
    })
    @PostMapping(value = "/delete")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(DELETE_K8S_NAMESPACE_BY_ID_ERROR)
    public Result delNamespaceById(@Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                   @RequestParam(value = "id") int id) {
        Map<String, Object> result = k8sNamespaceService.deleteNamespaceById(loginUser, id);
        return returnDataList(result);
    }

    /**
     * query unauthorized namespace
     *
     * @param loginUser login user
     * @param userId    user id
     * @return the namespaces which user have not permission to see
     */
    @Operation(summary = "queryUnauthorizedNamespace", description = "QUERY_UNAUTHORIZED_NAMESPACE_NOTES")
    @Parameters({
            @Parameter(name = "userId", description = "USER_ID", schema = @Schema(implementation = int.class, example = "100"))
    })
    @GetMapping(value = "/unauth-namespace")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_UNAUTHORIZED_NAMESPACE_ERROR)
    public Result queryUnauthorizedNamespace(@Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                             @RequestParam("userId") Integer userId) {
        Map<String, Object> result = k8sNamespaceService.queryUnauthorizedNamespace(loginUser, userId);
        return returnDataList(result);
    }

    /**
     * query unauthorized namespace
     *
     * @param loginUser login user
     * @param userId    user id
     * @return namespaces which the user have permission to see
     */
    @Operation(summary = "queryAuthorizedNamespace", description = "QUERY_AUTHORIZED_NAMESPACE_NOTES")
    @Parameters({
            @Parameter(name = "userId", description = "USER_ID", schema = @Schema(implementation = int.class, example = "100"))
    })
    @GetMapping(value = "/authed-namespace")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_AUTHORIZED_NAMESPACE_ERROR)
    public Result queryAuthorizedNamespace(@Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                           @RequestParam("userId") Integer userId) {
        Map<String, Object> result = k8sNamespaceService.queryAuthorizedNamespace(loginUser, userId);
        return returnDataList(result);
    }

    /**
     * query namespace available
     *
     * @param loginUser login user
     * @return namespace list
     */
    @Operation(summary = "queryAvailableNamespaceList", description = "QUERY_AVAILABLE_NAMESPACE_LIST_NOTES")
    @GetMapping(value = "/available-list")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_CAN_USE_K8S_NAMESPACE_ERROR)
    public Result queryAvailableNamespaceList(@Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser) {
        List<K8sNamespace> result = k8sNamespaceService.queryNamespaceAvailable(loginUser);
        return success(result);
    }
}
