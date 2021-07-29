package kz.spt.api.revision;

import kz.spt.api.model.CurrentUser;
import kz.spt.api.model.SptRevEntity;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.context.SecurityContextHolder;

public class SptRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        SptRevEntity sptRevEntity = (SptRevEntity) revisionEntity;
        if(SecurityContextHolder.getContext().getAuthentication()!=null && SecurityContextHolder.getContext().getAuthentication().getPrincipal()!=null){
            if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if(currentUser!=null){
                    sptRevEntity.setUsername(currentUser.getUsername());
                }
            }
        }
    }
}
