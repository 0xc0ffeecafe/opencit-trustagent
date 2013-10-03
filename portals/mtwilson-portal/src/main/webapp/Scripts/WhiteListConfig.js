var configSaved = false;
var selectedBothBiosVMM = false;
var registerHost = false;
var configurationSaved = [];

$(function() {
	
	$('#openSourcesHostType').find('input:text').each(function() {
		$(this).blur(function() {
			fnValidateEmptyValue($(this).attr('id'));
		});
	});
	$('#vmwareHostType').find('input').each(function() {
		$(this).blur(function() {
			fnValidateEmptyValue($(this).attr('id'));
		});
	});
	
	$('#defineVMWareHostType').find('input').each(function() {
		$(this).attr('disabled','disabled');
	});
	$('#openSourcesHostType').find('input').each(function() {
		$(this).attr('disabled','disabled');
	});
	$('#uploadButtonID').find('input').each(function() {
		$(this).attr('disabled','disabled');
	});
    
    $('#citrixHostType').find('input').each(function() {
		$(this).blur(function() {
			fnValidateEmptyValue($(this).attr('id'));
		});
	});
    
	$('#citrixHostType').find('input').each(function() {
		$(this).attr('disabled','disabled');
	});
    $('#citrixHostType').show();
    
	fnChangehostType($('#MainContent_ddlHOSTType'),true);
    
                $('#RegisterWhiteListHost').attr('disabled', 'disabled');
                $('#RegisterWhiteListHost').attr('checked', false);    
});


function fnUploadWhiteListConfigurationData() {

    $('#whiteListMessage').html('');
    var validation = false;
    var hostVo = new RegisterHostVo();

    if (checkForPCRConstrain('Oem_Bios_Checkbox', 'biosPCRsValues', 'OEM BIOS') && checkForPCRConstrain('Hypervisor_Checkbox', 'vmmPCRsValues', 'VMM')) {

        fnGetWhiteListConfigData();
        if ($('#MainContent_ddlHOSTType').val().toLowerCase().indexOf('vmware') >= 0) {
            hostVo.hostType = 'vmware'
        } else if (($('#MainContent_ddlHOSTType').val().toLowerCase().indexOf('kvm') >= 0) ||
                ($('#MainContent_ddlHOSTType').val().toLowerCase().indexOf('xen') >= 0)) {
            hostVo.hostType = 'intel'
        } else if (($('#MainContent_ddlHOSTType').val().toLowerCase().indexOf('citrix')) >= 0) {
            hostVo.hostType = 'citrix'
        }
        // alert(hostVo.hostType);
        //hostVo.hostType=$('#MainContent_ddlHOSTType').val();
        hostVo.status = null;
        // at this point isVMWare = 1 == VMWare, 2 == Citrix, 0 == TA    // TODO need to change this logic to just use the vendor name instead of 0..2
        // get citrix values
        if (isVMWare == 2) {
            if (fnValidateIpAddress($('#whiteListCitrix_Host').val())) {
                var valid0 = fnValidateEmptyValue('whiteListCitrix_Host');
                var valid1 = fnValidateEmptyValue('whiteListCitrix_portNO');
                var valid2 = fnValidateEmptyValue('whiteListCitrix_userName');
                var valid3 = fnValidateEmptyValue('whiteListCitrix_password');

                if (valid0 && valid1 && valid2 && valid3) {
                    validation = true;
                    hostVo.vmWareType = false;
                    hostVo.hostType = "citrix";
                    hostVo.hostName = $('#whiteListCitrix_Host').val();
                    hostVo.hostPortNo = $('#whiteListCitrix_portNO').val();
                    hostVo.vCenterString = "https://" + $('#whiteListCitrix_Host').val() + ":" + $('#whiteListCitrix_portNO').val() +
                            "/;" + $('#whiteListCitrix_userName').val() + ";" + $('#whiteListCitrix_password').val();
                }
            } else {
                alert("Please enter a valid ip address and try again.");
            }
        } else if (isVMWare == 1) { // get VMWare values
            if (fnValidateIpAddress($('#whiteListVMWare_vCenterServer').val())) {
                var valid0 = fnValidateEmptyValue('whiteListVMware_Host');
                var valid1 = fnValidateEmptyValue('whiteListVMWare_vCenterServer');
                var valid2 = fnValidateEmptyValue('whiteListVMWare_LoginID');
                var valid3 = fnValidateEmptyValue('whiteListVMWare_password');

                if (valid0 && valid1 && valid2 && valid3) {
                    validation = true;
                    hostVo.vmWareType = true;
                    hostVo.hostType = "vmware";
                    hostVo.vCenterString = getVCenterServerAddress('whiteListVMWare_vCenterServer') + ";" + $('#whiteListVMWare_LoginID').val() + ";" + $('#whiteListVMWare_password').val();
                    hostVo.hostName = $('#whiteListVMware_Host').val();
                    ;
                    hostVo.hostPortNo = null;
                }
            } else {
                alert("Please enter a valid ip address and try again.");
            }
        } else { // TA

            if (fnValidateIpAddress($('#whiteListOpenSource_Host').val())) {

                var valid1 = fnValidateEmptyValue('whiteListOpenSource_Host');
                var valid2 = fnValidateEmptyValue('whiteListOpenSource_portNO');

                if (valid1 && valid2) {
                    validation = true;
                    hostVo.vmWareType = false;
                    hostVo.vCenterString = "";
                    hostVo.hostType = "intel";
                    hostVo.hostName = $('#whiteListOpenSource_Host').val();
                    hostVo.hostPortNo = $('#whiteListOpenSource_portNO').val();

                    hostVo.biosWLTarget = null;
                    hostVo.vmmWLtarget = null;
                    hostVo.registered = false;
                }
            } else {
                alert("Please enter a valid ip address and try again.");
            }
        }

        if (validation) {
            var data = "registerHostVo=" + $.toJSON(hostVo) + "&biosWLTagrget=" + configurationSaved.biosWLTarget + "&vmmWLTarget=" + configurationSaved.vmmWLTarget;
            var config = new fnWhiteListConfig();
            config = configurationSaved.whiteListConfig;
            var regHost = $('#RegisterWhiteListHost').attr('checked') == 'checked' ? true : false;
            config.overWriteWhiteList = $('#OverwriteWhitelist').attr('checked') == 'checked' ? true : false;
                    
            if (selectedBothBiosVMM) {
                //fnOpenDialogWithYesNOButton("Do you want the host to be registered ?", "Confirm", 280, 150, fnSendWhiteListWithRegisterHostTrue, fnSendWhiteListWithRegisterHostFalse,data,config);
                if (regHost) {
                    config.registerHost = true;

                    registerHost = true;
                    $('#mainLoadingDiv').prepend(disabledDiv);
                    sendJSONAjaxRequest(false, 'getData/uploadWhiteListConfiguration.html', data + "&whiteListConfigVO=" + $.toJSON(config), fnUploadWhiteListSuccess, null);
                } else {
                    config.registerHost = false;
                    registerHost = false;
                    $('#mainLoadingDiv').prepend(disabledDiv);
                    sendJSONAjaxRequest(false, 'getData/uploadWhiteListConfiguration.html', data + "&whiteListConfigVO=" + $.toJSON(config), fnUploadWhiteListSuccess, null);
                }
                /*if(confirm("Do you want Host to be register ?")){
                 }*/
            } else {
                $('#mainLoadingDiv').prepend(disabledDiv);
                sendJSONAjaxRequest(false, 'getData/uploadWhiteListConfiguration.html', data + "&whiteListConfigVO=" + $.toJSON(config), fnUploadWhiteListSuccess, null);
                //return false;
            }
        }
    }
}
// Functions no longer needed because we no longer pop up and ask a user if he wants to register a host
//This method will set resigterHost option to true for fnWhiteListConfig object and send data to upload White List configuration to server.
//function fnSendWhiteListWithRegisterHostTrue(data,config) {
//	config.registerHost = true;
//        registerHost = true;        
//	$('#mainLoadingDiv').prepend(disabledDiv);
//	sendJSONAjaxRequest(false, 'getData/uploadWhiteListConfiguration.html', data+"&whiteListConfigVO="+$.toJSON(config), fnUploadWhiteListSuccess, null);
//}
//This method will set resigterHost option to false for fnWhiteListConfig object, called when user click NO while confirmation dialog box.
//function fnSendWhiteListWithRegisterHostFalse(data,config) {
//	config.registerHost = false;
//        registerHost = false;        
//	$('#mainLoadingDiv').prepend(disabledDiv);
//	sendJSONAjaxRequest(false, 'getData/uploadWhiteListConfiguration.html', data+"&whiteListConfigVO="+$.toJSON(config), fnUploadWhiteListSuccess, null);
//	
//}

//called for the response for fnSendWhiteListWithRegisterHostFalse/fnSendWhiteListWithRegisterHostTrue (upload white list button)
function fnUploadWhiteListSuccess(responseJSON) {
	$('#disabledDiv').remove();
	if (responseJSON.result) {
		fnClearAllFiled();
                if (registerHost) {
                    $('#whiteListMessage').html('<div class="successMessage">White List configuration updated successfully and the host has been registered.</div>');
                } else {
                    $('#whiteListMessage').html('<div class="successMessage">White List configuration updated successfully.</div>');
                }
	}else {
		$('#whiteListMessage').html('<div class="errorMessage">'+responseJSON.message+'</div>');
	}
        // Clear the registerHost flag as well
        registerHost = false;        
}

function fnClearAllFiled() {
	$('#whiteListMessage').html('');
	$('#mainLoadingDiv').find('input:text').each(function() {
		$(this).val('');
	});
	$('#mainLoadingDiv').find('input:password').each(function() {
		$(this).val('');
	});
}


function showDialogConfigureWhiteHelp() {
	var str="";
	for ( var iteam in configureWhiteHelp) {
		str+='<div class="helpDiv">'+configureWhiteHelp[iteam]+'</div>';
	}
	fnOpenDialog(str, "Help", 500, 200,false);
}

function showDialogWhiteListApplicableHelp() {
	var str="";
	for ( var iteam in applicableWhiteListTargetHelp) {
		str+='<div class="helpDiv">'+applicableWhiteListTargetHelp[iteam]+'</div>';
	}
	fnOpenDialog(str, "Help", 600, 350,false);
}
function showDialogRequiredPCRValues() {
	var str="";
	for ( var iteam in requiredPCRValuesHelp) {
		str+='<div class="helpDiv">'+requiredPCRValuesHelp[iteam]+'</div>';
	}
	fnOpenDialog(str, "Help", 500, 350,false);
}
/* Soni_Begin_25/09/2012_help icon */

function showDialogVcenterHelp() {
	var str="";
	for ( var iteam in vCenterStringHelp) {
		str+='<div class="helpDiv">'+vCenterStringHelp[iteam]+'</div>';
	}
	fnOpenDialog(str, "Help", 500,275,false);
}
/* Soni_Begin_25/09/2012_help icon */
//function to display help for adding location while while list configuration
function showHelpForLocation() {
	var str="";
	for ( var iteam in addLocationHelp) {
		str+='<div class="helpDiv">'+addLocationHelp[iteam]+'</div>';
	}
	fnOpenDialog(str, "Help", 500, 200,false);
}


function checkForPCRConstrain(checkBoxID,pcrID,textValue) {
	if ($('#'+checkBoxID).attr('checked') == 'checked') {
		var check = false;
		$('#'+pcrID).find('input:checkbox').each(function() {
			if($(this).attr('checked') == 'checked'){
				check =true;
			}
		});
		if (!check) {
			$('#whiteListMessage').html('<div class="errorMessage"> Please select at least one PCR for '+textValue+".</div>");
			return false;
		}
	}
	return true;
}


//function to clear selection of White List Configuration, on change of Host Type
function fnClearAllConfigFiled() {
	$('#mainLoadingDiv').find('input:text,input:password').each(function() {
		$(this).val('');
	});
	
	$('#mainDivForConfig').find('input:checkbox').each(function() {
		$(this).attr('checked',false);
	});
	$('#mainDivForConfig').find('select').each(function() {
		$(this).find('option').attr('selected',false);
	});
}

//function to select corresponding BIOS/VMM white list config checkbox, on Selection of White List Applicable For
function fnSelectWhiteListType(element, checkBoxID) {
    $('#' + checkBoxID).attr('checked', 'checked');
    //if ($('#Hypervisor_bios_applicable_for').children("option:selected").text() == "Global") {
    //    $("#required_pcrs_17").attr('checked',false);
    //} 
    //fnChangeApplicableFor(checked,'Hypervisor_bios_applicable_for','Oem_Bios_Checkbox');
    fnDisableOrEnableUploadButton(true);
}



function fnGetWhiteListConfigData() {
	configurationSaved = [];
	selectedBothBiosVMM = false;
	var config = new fnWhiteListConfig();
	config.biosWhiteList = $('#Oem_Bios_Checkbox').attr('checked') == 'checked'? true : false ;
	config.vmmWhiteList = $('#Hypervisor_Checkbox').attr('checked') == 'checked'? true : false ;
	config.biosWLTarget = null;
	config.vmmWLTarget = null;
	var str = "";
	$('#biosPCRsValues').find('input:checked').each(function() {
		str+=$(this).attr('name')+",";
	});
	str = str.substring(0, str.length-1);
	config.biosPCRs = str;
	
	str = "";
	$('#vmmPCRsValues').find('input:checked').each(function() {
		str+=$(this).attr('name')+",";
	});
	str = str.substring(0, str.length-1);
	config.vmmPCRs = str;
	
	
	config.hostLocation = null;//$('#location_host').val();
	config.registerHost = false;
	if (config.biosWhiteList && config.vmmWhiteList) {
		selectedBothBiosVMM = true;
	}
	configurationSaved.whiteListConfig=config;
	configurationSaved.biosWLTarget=$('#oem_bios_applicable_for').val();
	configurationSaved.vmmWLTarget=$('#Hypervisor_bios_applicable_for').val();
	configurationSaved.result=true;
	//return "whiteListConfigVO="+$.toJSON(config)+"&biosWLTagrget="+$('#oem_bios_applicable_for').val()+"&vmmWLTarget="+$('#Hypervisor_bios_applicable_for').val();
}

//function to select required PCR Values While changing HostType in White List Configuration Page.
function changeRequiredPCR(){
	var requiredPCR="";
	$('#MainContent_ddlHOSTType').find('option').each(function() {
		if($(this).text()==$('#MainContent_ddlHOSTType').val()){
			requiredPCR = $(this).attr("pcrs");
		}
	});
	if (requiredPCR.length > 0) {
		if (requiredPCR.indexOf(",") > 0) {
			var pcrs = requiredPCR.split(",");
			for ( var pcr in pcrs) {
				$('#required_pcrs_'+pcrs[pcr]).attr('checked','checked');
			}
		}else{
			$('#required_pcrs_'+requiredPCR).attr('checked','checked');
		}
	
	}
}

//function to select/unselect the White List Applicable for while checking the BIOS/VMM type white list.
function fnChangeApplicableFor(status,selectID,secondCheckBoxID) {
	if (status) {
		$('#'+selectID).find('option:eq(0)').attr('selected','selected');
		fnDisableOrEnableUploadButton(status);
	}else {
		$('#'+selectID).find('option').each(function() {
			$(this).attr('selected',false);
		});
		if ($('#'+secondCheckBoxID).attr('checked') != 'checked') {
			fnDisableOrEnableUploadButton(status);
		}
	}
    
            if (($('#Oem_Bios_Checkbox').attr('checked') == 'checked') && ($('#Hypervisor_Checkbox').attr('checked') == 'checked')) {
                // Enable the register host checkbox with default value set to unchecked
                $('#RegisterWhiteListHost').attr('disabled', false);
            } else {
                $('#RegisterWhiteListHost').attr('disabled', true);
                $('#RegisterWhiteListHost').attr('checked', false);
            }
                
}

//function to check the conditions for enabling/disabling "upload white list" button
function fnDisableOrEnableUploadButton(checkBox) {
    var status = !checkBox;
    $('#defineVMWareHostType').find('input').each(function() {
        $(this).attr('disabled', status);
    });
    $('#openSourcesHostType').find('input').each(function() {
        $(this).attr('disabled', status);
    });
    $('#citrixHostType').find('input').each(function() {
        $(this).attr('disabled', status);
    });
    $('#uploadButtonID').find('input').each(function() {
        $(this).attr('disabled', status);
    });
}
