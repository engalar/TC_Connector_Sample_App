package system;

import com.mendix.core.actionmanagement.IActionRegistrator;

public class UserActionsRegistrar
{
  public void registerActions(IActionRegistrator registrator)
  {
    registrator.bundleComponentLoaded();
    registrator.registerUserAction(system.actions.VerifyPassword.class);
    registrator.registerUserAction(tcconnector.actions.__SSOCallBackRequest.class);
    registrator.registerUserAction(tcconnector.actions.CallTeamcenterService.class);
    registrator.registerUserAction(tcconnector.actions.CloseBOMWindows.class);
    registrator.registerUserAction(tcconnector.actions.CreateBOMWindows.class);
    registrator.registerUserAction(tcconnector.actions.CreateBOMWindows2.class);
    registrator.registerUserAction(tcconnector.actions.CreateObject.class);
    registrator.registerUserAction(tcconnector.actions.CreateRelation.class);
    registrator.registerUserAction(tcconnector.actions.CreateWorkflow.class);
    registrator.registerUserAction(tcconnector.actions.DownloadFiles.class);
    registrator.registerUserAction(tcconnector.actions.ExecuteSavedQueries.class);
    registrator.registerUserAction(tcconnector.actions.ExpandGRMRelationsForPrimary.class);
    registrator.registerUserAction(tcconnector.actions.ExpandGRMRelationsForSecondary.class);
    registrator.registerUserAction(tcconnector.actions.ExpandPSAllLevels.class);
    registrator.registerUserAction(tcconnector.actions.ExpandPSOneLevel.class);
    registrator.registerUserAction(tcconnector.actions.ExpandPSOneLevel2.class);
    registrator.registerUserAction(tcconnector.actions.FindSavedQueries.class);
    registrator.registerUserAction(tcconnector.actions.FindUsersTasks.class);
    registrator.registerUserAction(tcconnector.actions.GetAllTasks.class);
    registrator.registerUserAction(tcconnector.actions.GetAttachedLOV.class);
    registrator.registerUserAction(tcconnector.actions.GetAvailableDatasetTypes.class);
    registrator.registerUserAction(tcconnector.actions.GetFileTypesForDatasetType.class);
    registrator.registerUserAction(tcconnector.actions.GetInitialLOVValues.class);
    registrator.registerUserAction(tcconnector.actions.GetItemFromId.class);
    registrator.registerUserAction(tcconnector.actions.GetNextLOVValues.class);
    registrator.registerUserAction(tcconnector.actions.GetProperties.class);
    registrator.registerUserAction(tcconnector.actions.GetRevisionRules.class);
    registrator.registerUserAction(tcconnector.actions.GetTcSessionInfo.class);
    registrator.registerUserAction(tcconnector.actions.GetTcSessionInformation.class);
    registrator.registerUserAction(tcconnector.actions.GetVariantRule.class);
    registrator.registerUserAction(tcconnector.actions.GetWorkflowTemplates.class);
    registrator.registerUserAction(tcconnector.actions.Login.class);
    registrator.registerUserAction(tcconnector.actions.Logout.class);
    registrator.registerUserAction(tcconnector.actions.PerformAction.class);
    registrator.registerUserAction(tcconnector.actions.PerformGeneralQuerySearchAW.class);
    registrator.registerUserAction(tcconnector.actions.PerformItemSimpleQuerySearchAW.class);
    registrator.registerUserAction(tcconnector.actions.PerformSearch.class);
    registrator.registerUserAction(tcconnector.actions.RetrieveCookie.class);
    registrator.registerUserAction(tcconnector.actions.ReviseObjects.class);
    registrator.registerUserAction(tcconnector.actions.SetProperties.class);
    registrator.registerUserAction(tcconnector.actions.UploadFiles.class);
    registrator.registerUserAction(tcconnector.actions.UploadTemporaryFiles.class);
    registrator.registerUserAction(tcconnector.actions.WhereUsed.class);
    registrator.registerUserAction(tcconnectorsample.actions.SplitString.class);
    registrator.registerUserAction(viewer3d.actions.VisServerAction.class);
    registrator.registerUserAction(viewer3d_tc.actions.__GenerateBOMLineQueryInput.class);
    registrator.registerUserAction(viewer3d_tc.actions.VisServerAction_TC.class);
  }
}
