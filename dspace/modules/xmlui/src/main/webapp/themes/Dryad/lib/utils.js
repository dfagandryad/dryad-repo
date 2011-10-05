jQuery(document).ready(function(){
    initdatasetsubmissionfile();
    initjQueryTooltips();
    initCiteMe();
});


function initdatasetsubmissionfile() {
    //For each file I find make sure that the form gets auto submitted
    jQuery('input#aspect_submission_StepTransformer_field_dataset-file').bind('click', function(){
        jQuery("input[type=radio][name='datafile_type'][value='file']").click();
        enableEmbargo();
    });

    jQuery('#aspect_submission_StepTransformer_div_submit-describe-dataset').find(":input[type=file]").bind('change', function(){
        if(this.id == 'aspect_submission_StepTransformer_field_dataset-file'){
            //Make sure the title gets set with the filename
            var fileName = jQuery(this).val().substr(0, jQuery(this).val().lastIndexOf('.'));
            fileName = fileName.substr(fileName.lastIndexOf('\\') + 1, fileName.length);
            //Find our input & give it the value of our filename
            jQuery('input#aspect_submission_StepTransformer_field_dc_title').val(fileName);
        }
        //Now find our form
        var form = jQuery(this).closest("form");
        //Now that we have our, indicate that I want it processed BUT NOT TO CONTINUE, just upload
        //We do this by adding a param to the form action
        form.attr('action', form.attr('action') + '?processonly=true');
        //Now we submit our form
        form.submit();
    });

    jQuery("input[type='radio'][name='datafile_type']").change(function(){
        //If an external reference is selected we need to disable our embargo
        if(jQuery(this).val() == 'identifier'){
            disableEmbargo();
        }else{
            enableEmbargo();
        }

    });

    //Should we have a data file with an external repo then disable the embargo
    if(jQuery("input[name='disabled-embargo']").val()){
        disableEmbargo();
    }
    jQuery('select#aspect_submission_StepTransformer_field_datafile_repo').bind('change', function(){
        if(jQuery(this).val() == 'other'){
            jQuery("input[name='other_repo_name']").show();
        }else{
            jQuery("input[name='other_repo_name']").hide();
        }
    });
    

    //For our identifier a hint needs to be created
    var dataFileIdenTxt = jQuery('input#aspect_submission_StepTransformer_field_datafile_identifier');
    dataFileIdenTxt.inputHints();
    var repoNameTxt = jQuery('input#aspect_submission_StepTransformer_field_other_repo_name');
    repoNameTxt.inputHints();
    dataFileIdenTxt.blur(function(){
        if(jQuery(this).attr('title') != jQuery(this).val())
            jQuery('input#aspect_submission_StepTransformer_field_dc_title').val(jQuery(this).val());
    });

    jQuery('form#aspect_submission_StepTransformer_div_submit-describe-dataset').submit(function() {
        var dataFileIdenTxt = jQuery('input#aspect_submission_StepTransformer_field_datafile_identifier');
        if(dataFileIdenTxt.val() == dataFileIdenTxt.attr('title'))
            dataFileIdenTxt.val('');

        var repoNameTxt = jQuery('input#aspect_submission_StepTransformer_field_other_repo_name');
        if(repoNameTxt.val() == repoNameTxt.attr('title'))
            repoNameTxt.val('');


        jQuery('select#aspect_submission_StepTransformer_field_dc_type_embargo').removeAttr('disabled');

        return true;
    });

}

function disableEmbargo(){
    var embargoSelect = jQuery('select#aspect_submission_StepTransformer_field_dc_type_embargo');
    embargoSelect.find('option:selected').removeAttr('selected');
    //Select the publish immediately option
    embargoSelect.find("option[value='none']").attr('selected', 'selected');
    embargoSelect.attr('disabled', 'disabled');
}

function enableEmbargo(){
    var embargoSelect = jQuery('select#aspect_submission_StepTransformer_field_dc_type_embargo');
    embargoSelect.removeAttr('disabled');
}

function initjQueryTooltips(){

    jQuery('fieldset#aspect_submission_StepTransformer_list_submit-select-publication *').tooltip();
    jQuery('fieldset#aspect_submission_StepTransformer_list_submit-describe-dataset *').tooltip();
}

function initCiteMe() {
    jQuery('#citemediv').hide();
    jQuery('#sharemediv').hide();
    
    jQuery('#cite').click(function(){
    	jQuery('#citemediv').toggle();
    	return false;
    });
    
    jQuery('#share').click(function(){
    	jQuery('#sharemediv').toggle();
    	return false;
    });
}


jQuery.fn.inputHints = function() {
   // hides the input display text stored in the title on focus
   // and sets it on blur if the user hasn't changed it.

   // show the display text
   this.each(function(i) {
       jQuery(this).val(jQuery(this).attr('title'));
       jQuery(this).addClass('inputhint');
   });

   // hook up the blur & focus
   this.focus(function() {
       if (jQuery(this).val() == jQuery(this).attr('title')){
           jQuery(this).val('');
           jQuery(this).removeClass('inputhint');
           //Select the correct radio button
           jQuery("input[type=radio][name='datafile_type'][value='identifier']").click();
           if(this.name == 'datafile_identifier')
                disableEmbargo();
       }
   }).blur(function() {
       if (jQuery(this).val() == ''){
           jQuery(this).val(jQuery(this).attr('title'));
           jQuery(this).addClass('inputhint');
       }
   });
};